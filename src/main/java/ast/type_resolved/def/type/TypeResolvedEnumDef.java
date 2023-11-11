package ast.type_resolved.def.type;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.objects.GetField;
import ast.ir.instruction.objects.MethodCall;
import ast.ir.instruction.objects.SetField;
import ast.ir.instruction.stack.Pop;
import ast.ir.instruction.stack.Push;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.typed.def.field.BuiltinFieldDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.EnumDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.ArrayType;
import builtin_types.types.MaybeUninit;
import builtin_types.types.UnitType;
import builtin_types.types.numbers.IntegerType;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import runtime.Unit;
import util.LateInit;
import util.ListUtils;

import java.math.BigInteger;
import java.util.List;


public record TypeResolvedEnumDef(Loc loc, String name, boolean nested, List<TypeResolvedEnumProperty> properties, List<TypeResolvedEnumElement> elements, List<SnuggleTypeResolvedMethodDef> methods) implements TypeResolvedTypeDef {

    @Override
    public void verifyGenericCounts(GenericVerifier verifier) throws CompilationException {
        for (TypeResolvedEnumProperty property : properties)
            verifier.verifyType(property.type(), loc);
        for (SnuggleTypeResolvedMethodDef methodDef : methods)
            methodDef.verifyGenericCounts(verifier);
    }

    @Override
    public TypeDef instantiate(TypeDef currentType, TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        if (generics.size() != 0)
            throw new IllegalStateException("Enum should not have generics? Bug in compiler, please report!");
        TypeDef.InstantiationStackFrame newStackFrame = new TypeDef.InstantiationStackFrame(instantiationLoc, currentType, cause);
        List<FieldDef> fieldDefs = getFields(currentType, checker, generics, newStackFrame);
        List<MethodDef> methodDefs = getMethods(fieldDefs, currentType, checker, generics, newStackFrame);
        return new EnumDef(loc, name, elements.size(), fieldDefs, methodDefs);
    }

    private List<FieldDef> getFields(TypeDef currentType, TypeChecker checker, List<TypeDef> generics, TypeDef.InstantiationStackFrame cause) {
        //First, the arrays holding the property values
        //Second, the enum elements
        return ListUtils.join(
                ListUtils.mapIndexed(properties, (property, index) -> new BuiltinFieldDef(
                    staticArrayName(property, index),
                    currentType,
                    checker.getGenericBuiltin(ArrayType.INSTANCE, List.of(checker.getOrInstantiate(property.type(), generics, loc, cause)), loc, cause),
                    true
                )),
                ListUtils.mapIndexed(elements, (element, index) -> new BuiltinFieldDef(
                        element.name,
                        currentType,
                        currentType,
                        true
                ))
        );

    }

    private List<MethodDef> getMethods(List<FieldDef> fieldDefs, TypeDef currentType, TypeChecker checker, List<TypeDef> generics, TypeDef.InstantiationStackFrame cause) {

        TypeDef u32 = checker.getBasicBuiltin(IntegerType.U32);

        //Choose the element type, for index() to return
        TypeDef elementType;
        if (elements.size() > 1 << 16)
            elementType = u32;
        else if (elements.size() > 1 << 8)
            elementType = checker.getBasicBuiltin(IntegerType.U16);
        else
            elementType = checker.getBasicBuiltin(IntegerType.U8);

        LateInit<CodeBlock, CompilationException> staticInitBlockLazy = new LateInit<>(() -> {
            //Create the static init method
            CodeBlock staticInitBlock = new CodeBlock((MethodDef) null);

            //Create all the property arrays
            BigInteger numElements = BigInteger.valueOf(elements.size());
            int numProperties = properties.size();
            TypeDef[] propertyTypes = new TypeDef[numProperties];
            MethodDef[] propertyArraySetters = new MethodDef[numProperties];
            for (int i = 0; i < numProperties; i++) {
                //Get property
                TypeResolvedEnumProperty property = properties.get(i);
                TypeDef propertyType = checker.getOrInstantiate(property.type, generics, loc, cause);
                //Push the size
                staticInitBlock.emit(new Push(cause, loc, numElements, u32));
                //Call the "new array" method...
                TypeDef arrayType = checker.getGenericBuiltin(ArrayType.INSTANCE, List.of(propertyType), loc, cause);
                MethodDef newMethod;
                if ((newMethod = ListUtils.find(arrayType.methods(), m -> m.name().equals("new"))) != null) {
                    //Has a new method, so let's call it
                    staticInitBlock.emit(new MethodCall(false, newMethod, List.of()));
                    propertyTypes[i] = propertyType;
                    propertyArraySetters[i] = ListUtils.find(arrayType.methods(), m -> m.name().equals("set"));
                } else {
                    //No new() method, so wrap in a MaybeUninit
                    TypeDef maybeUninit = checker.getGenericBuiltin(MaybeUninit.INSTANCE, List.of(propertyType), loc, cause);
                    arrayType = checker.getGenericBuiltin(ArrayType.INSTANCE, List.of(maybeUninit), loc, cause);
                    if ((newMethod = ListUtils.find(arrayType.methods(), m -> m.name().equals("new"))) != null) {
                        //Has a new method, so let's call it
                        staticInitBlock.emit(new MethodCall(false, newMethod, List.of()));
                        propertyTypes[i] = propertyType;
                        propertyArraySetters[i] = ListUtils.find(arrayType.methods(), m -> m.name().equals("set"));
                    } else {
                        throw new IllegalStateException("valuetypeify should have produced something such that Array<valuetypeify(propertyType)> had a \"new\" method. Bug in compiler, please report!");
                    }
                }
                //Store the array in the corresponding static field
                staticInitBlock.emit(new SetField(List.of(fieldDefs.get(i))));
            }

            //Fill the property arrays with the elements, also fill the element values
            for (int i = 0; i < elements.size(); i++) {
                TypeResolvedEnumElement element = elements.get(i);
                BigInteger elementIndex = BigInteger.valueOf(i);

                for (int j = 0; j < numProperties; j++) {
                    //Push the array
                    staticInitBlock.emit(new GetField(List.of(fieldDefs.get(j))));
                    //Push the index
                    staticInitBlock.emit(new Push(cause, loc, elementIndex, u32));
                    //Compile the element's jth arg
                    element.args.get(j).check(currentType, checker, generics, propertyTypes[j], cause).compile(staticInitBlock, null);
                    //Store in array
                    staticInitBlock.emit(new MethodCall(false, propertyArraySetters[j], List.of()));
                    //Pop
                    staticInitBlock.emit(new Pop(propertyArraySetters[j].returnType()));
                }

                //Set element value
                FieldDef field = fieldDefs.get(i + numProperties);
                //Push element value
                staticInitBlock.emit(new Push(cause, loc, elementIndex, elementType));
                //Set into field
                staticInitBlock.emit(new SetField(List.of(field)));
            }

            //Finally push unit
            staticInitBlock.emit(new Push(cause, loc, Unit.INSTANCE, checker.getBasicBuiltin(UnitType.INSTANCE)));

            return staticInitBlock;
        });

        //Return the methods
        return ListUtils.join(
                //The actual methods which are defined in the enum body
                ListUtils.map(methods, m -> m.instantiateType(methods, currentType, checker, generics, cause)),
                //Methods generated from the properties
                ListUtils.mapIndexed(properties, (property, index) -> {
                    MethodDef getter = ListUtils.find(fieldDefs.get(index).type().methods(), m -> m.name().equals("get"));
                    return new BytecodeMethodDef(property.name, false, currentType, List.of(), checker.getOrInstantiate(property.type(), generics, loc, cause), true, (b, d, v) -> {
                        //Store index as local
                        int localIndex = b.env.maxIndex();
                        v.visitVarInsn(Opcodes.ISTORE, localIndex);
                        //Get the desired array onto the stack
                        new GetField(List.of(fieldDefs.get(index))).accept(b, v);
                        //Load the index
                        v.visitVarInsn(Opcodes.ILOAD, localIndex);
                        //Call the array's "get" method
                        getter.compileCall(false, b, d, v);
                    });
                }),
                //Static initializer, which fills all the fields
                List.of(new BytecodeMethodDef("#init", true, currentType, List.of(), checker.getBasicBuiltin(UnitType.INSTANCE), true,
                        v -> staticInitBlockLazy.get().writeJvmBytecode(v),
                        new LateInit<>(() -> staticInitBlockLazy.get().cost()) //Custom cost
                )),
                //.index(), which does literally nothing at runtime lol, just converts between topLevelTypes
                List.of(new BytecodeMethodDef("index", false, currentType, List.of(), elementType, true, v -> {}, BytecodeMethodDef.ZERO)) //zero cost
        );
    }

    private static String staticArrayName(TypeResolvedEnumProperty property, int index) {
        return "#array_" + index + "_name_" + property.name;
    }

    @Override
    public int numGenerics() {
        return 0; //No generic enums
    }

    public record TypeResolvedEnumProperty(String name, ResolvedType type) {}
    public record TypeResolvedEnumElement(String name, List<TypeResolvedExpr> args) {}

}

