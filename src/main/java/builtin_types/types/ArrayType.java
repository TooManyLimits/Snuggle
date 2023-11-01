package builtin_types.types;

import ast.ir.helper.BytecodeHelper;
import ast.passes.TypeChecker;
import ast.typed.def.field.BuiltinFieldDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;

public class ArrayType implements BuiltinType {

    public static final ArrayType INSTANCE = new ArrayType();
    private ArrayType() {}

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics) {
        TypeDef type = checker.getGenericBuiltin(this, generics);
        TypeDef elementType = generics.get(0);
        TypeDef u32 = checker.getBasicBuiltin(IntegerType.U32);
        TypeDef unit = checker.getBasicBuiltin(UnitType.INSTANCE);

        ArrayList<MethodDef> methods = new ArrayList<>();

        if (elementType.isPlural()) {
            //Arrays of plurals are broken up into multiple smaller arrays
            List<TypeDef> flattenedElems = flattenedElems(elementType);
            int stackSlots = flattenedElems.size();
            boolean containsNonOptionalReferenceType = containsNonOptionalReferenceType(elementType);

            //Methods
            if (!containsNonOptionalReferenceType) {
                //If there's no non-optional reference type, then we can add a "new" method...
                methods.add(new BytecodeMethodDef("new", false, type, List.of(u32), unit, true, v -> {
                    //Stack is [size]
                    BytecodeHelper.newArray(v, elementType); //[arrays, size]
                    v.visitInsn(Opcodes.POP); //[arrays]
                }));
            }
            methods.add(new BytecodeMethodDef("size", false, type, List.of(), u32, true, v -> {
                //Stack is [arrays]
                //Pop all the arrays except for 1, then do ARRAYLENGTH
                for (int i = 0; i < stackSlots - 1; i++)
                    v.visitInsn(Opcodes.POP);
                v.visitInsn(Opcodes.ARRAYLENGTH);
            }));
            methods.add(new BytecodeMethodDef("get", false, type, List.of(u32), elementType, true, (b, d, v) -> {
                //Stack is [arr1, arr2, ..., index]
                //Want [arr1[index], arr2[index], ...]
                //If we have desiredFields, then only a certain sublist of this stack should be there in the end
                getArray(b.env.maxIndex(), flattenedElems.size() - 1, v, flattenedElems, getDesiredFieldRange(elementType, d)); //This will do that!
            }));
            methods.add(new BytecodeMethodDef("set", false, type, List.of(u32, elementType), elementType, true, (b, $, v) -> {
                //Stack is [arr1, arr2, ..., arrN, index, e1, e2, ... eN]
                //Want [e1, e2, ... eN], and arr1[index] = e1, arr2[index] = e2, ... arrN[index] = eN
                setArray(b.env.maxIndex(), v, flattenedElems);
            }));

            return methods;
        } else {
            //Element type is non-plural, just a regular array
            ArrayOpcodes opcodes = getOpcodes(elementType);

            //Only add the "new(count)" method if this array is primitive, or Option<ReferenceType>
            if (elementType.isOptionalReferenceType() || !opcodes.isReference)
                methods.add(new BytecodeMethodDef("new", false, type, List.of(u32), unit, true, v -> {
                    if (opcodes.isReference)
                        v.visitTypeInsn(Opcodes.ANEWARRAY, elementType.name());
                    else
                        v.visitIntInsn(Opcodes.NEWARRAY, opcodes.newOpcodeArg());
                }));
            methods.add(new BytecodeMethodDef("size", false, type, List.of(), u32, true, v -> {
                v.visitInsn(Opcodes.ARRAYLENGTH);
            }));
            methods.add(new BytecodeMethodDef("get", false, type, List.of(u32), elementType, true, v -> {
                v.visitInsn(opcodes.loadOpcode);
            }));
            methods.add(new BytecodeMethodDef("set", false, type, List.of(u32, elementType), elementType, true, v -> {
                v.visitInsn(opcodes.dupOpcode);
                v.visitInsn(opcodes.storeOpcode);
            }));
            //If this is Option<ReferenceType>, then also allow "set" with non-optional param
            if (elementType.isOptionalReferenceType()) {
                TypeDef innerType = ((BuiltinTypeDef) elementType.get()).generics.get(0);
                methods.add(new BytecodeMethodDef("set", false, type, List.of(u32, innerType), innerType, true, v -> {
                    v.visitInsn(opcodes.dupOpcode);
                    v.visitInsn(opcodes.storeOpcode);
                }));
            }

        }

        return methods;
    }

    @Override
    public List<FieldDef> getFields(TypeChecker checker, List<TypeDef> generics) {
        TypeDef thisType = checker.getGenericBuiltin(this, generics);
        TypeDef elementType = generics.get(0);
        if (elementType.isPlural()) {
            return ListUtils.map(ListUtils.filter(elementType.fields(),
                            f -> !f.isStatic()),
                    f -> new BuiltinFieldDef(
                            "#array_" + f.name(),
                            thisType,
                            checker.getGenericBuiltin(ArrayType.INSTANCE, List.of(f.type())),
                            false
                    )
            );
        } else {
            return List.of();
        }
    }


    //Stack is [arr1, arr2, ..., index]
    //Want: [arr1[index], arr2[index], ...] (unless desiredFields dictates otherwise)
    //curLocal should start at block.maxIndex(), it increments
    //curElem should start at flattenedElemTypes.size() - 1, it decrements.
    private static void getArray(int curLocal, int curElem, MethodVisitor jvm, List<TypeDef> flattenedElemTypes, int[] desiredFieldRange) {
        if (curElem == desiredFieldRange[0] - 1) {
            jvm.visitInsn(Opcodes.POP2);
            getArray(curLocal, curElem - 1, jvm, flattenedElemTypes, desiredFieldRange);
            return;
        } else if (curElem < desiredFieldRange[0]) {
            jvm.visitInsn(Opcodes.POP);
            getArray(curLocal, curElem - 1, jvm, flattenedElemTypes, desiredFieldRange);
            return;
        }

        ArrayOpcodes opcodes = ArrayType.getOpcodes(flattenedElemTypes.get(curElem));
        if (curElem == 0) {
            //[arr1, index] -> [arr1[index]], or [] if not desired.
            if (desiredFieldRange[0] > 0)
                jvm.visitInsn(Opcodes.POP2);
            else
                jvm.visitInsn(opcodes.loadOpcode);
        } else {
            if (curElem >= desiredFieldRange[1]) {
                jvm.visitInsn(Opcodes.SWAP);
                jvm.visitInsn(Opcodes.POP);
                getArray(curLocal, curElem - 1, jvm, flattenedElemTypes, desiredFieldRange);
                return;
            }

            //[arr1, arr2, ..., arrN, index]
            jvm.visitInsn(Opcodes.DUP_X1); //[arr1, arr2, ..., index, arrN, index]
            jvm.visitInsn(opcodes.loadOpcode); //[arr1, arr2, ..., index, arrN[index]]
            jvm.visitVarInsn(opcodes.varStoreOpcode, curLocal); //[arr1, arr2, ..., index]. Locals: curLocal -> arrN[index]
            getArray(
                    curLocal + flattenedElemTypes.get(curElem).stackSlots(), //Increment
                    curElem - 1, //Decrement
                    jvm,
                    flattenedElemTypes,
                    desiredFieldRange
            );
            //After the recursive call is done, now we load the value back from the local variable
            jvm.visitVarInsn(opcodes.varLoadOpcode, curLocal);
        }
    }

    //Get the range of the flattened types which are desired by this list
    //2 element array output, first inclusive, second exclusive
    private int[] getDesiredFieldRange(TypeDef type, List<FieldDef> desiredFields) {
        //If no specifically desired fields, return the whole thing as the range
        if (desiredFields.size() == 0)
            return new int[] { 0, flattenedElems(type).size() };
        //Otherwise, do some calculating
        int curIndex = 0;
        for (FieldDef def : type.fields()) {
            if (def.isStatic()) continue;
            if (def == desiredFields.get(0)) {
                int[] inner = getDesiredFieldRange(def.type(), desiredFields.subList(1, desiredFields.size()));
                return new int[] {inner[0] + curIndex, inner[1] + curIndex};
            }
            curIndex += flattenedElems(def.type()).size();
        }
        throw new IllegalStateException("Couldn't find desired field range? Should have caught this earlier while type-checking. Bug in compiler, please report!");
    }

    private static void setArray(int curLocal, MethodVisitor jvm, List<TypeDef> flattenedElemTypes) {
        //Store e1, ... eN into local variables.
        //[arr1, ... arrN, index, e1, ... eN]
        int minLocal = curLocal;
        for (int i = flattenedElemTypes.size() - 1; i >= 0; i--) {
            TypeDef elemType = flattenedElemTypes.get(i);
            jvm.visitVarInsn(getOpcodes(elemType).varStoreOpcode, curLocal);
            curLocal += elemType.stackSlots();
        }
        curLocal = minLocal;
        //[arr1, ..., arrN, index]
        //Store each element into array
        for (int i = flattenedElemTypes.size() - 1; i >= 0; i--) {
            //[arr1, ..., arrI, index]
            TypeDef elemType = flattenedElemTypes.get(i);
            ArrayOpcodes opcodes = getOpcodes(elemType);
            jvm.visitInsn(Opcodes.DUP_X1); //[arr1, ..., index, arrI, index]
            jvm.visitVarInsn(opcodes.varLoadOpcode, curLocal); //[arr1, ..., index, arrI, index, eI]
            curLocal += elemType.stackSlots();
            jvm.visitInsn(opcodes.storeOpcode); //[arr1, ..., arrI-1, index]
        }
        //[index]
        jvm.visitInsn(Opcodes.POP); //[]
        for (TypeDef elemType : flattenedElemTypes) {
            curLocal -= elemType.stackSlots();
            jvm.visitVarInsn(getOpcodes(elemType).varLoadOpcode, curLocal);
        }
        //[e1, e2, ..., eN]. Done!
    }


    //Get the proper store/load opcodes for this element type
    private record ArrayOpcodes(boolean isReference, int newOpcodeArg, int dupOpcode, int storeOpcode, int loadOpcode, int varStoreOpcode, int varLoadOpcode) {}
    private static ArrayOpcodes getOpcodes(TypeDef elementTypeDef) {
        boolean isReference = false;
        int newOpcodeArg = -1;
        int dupOpcode = Opcodes.DUP_X2;
        int storeOpcode, loadOpcode, varStoreOpcode, varLoadOpcode;
        if (elementTypeDef.builtin() instanceof IntegerType i) {
            switch (i.bits) {
                case 8 -> {
                    newOpcodeArg = Opcodes.T_BYTE;
                    storeOpcode = Opcodes.BASTORE;
                    loadOpcode = Opcodes.BALOAD;
                    varStoreOpcode = Opcodes.ISTORE;
                    varLoadOpcode = Opcodes.ILOAD;
                }
                case 16 -> {
                    newOpcodeArg = Opcodes.T_SHORT;
                    storeOpcode = Opcodes.SASTORE;
                    loadOpcode = Opcodes.SALOAD;
                    varStoreOpcode = Opcodes.ISTORE;
                    varLoadOpcode = Opcodes.ILOAD;
                }
                case 32 -> {
                    newOpcodeArg = Opcodes.T_INT;
                    storeOpcode = Opcodes.IASTORE;
                    loadOpcode = Opcodes.IALOAD;
                    varStoreOpcode = Opcodes.ISTORE;
                    varLoadOpcode = Opcodes.ILOAD;
                }
                case 64 -> {
                    dupOpcode = Opcodes.DUP2_X2;
                    newOpcodeArg = Opcodes.T_LONG;
                    storeOpcode = Opcodes.LASTORE;
                    loadOpcode = Opcodes.LALOAD;
                    varStoreOpcode = Opcodes.LSTORE;
                    varLoadOpcode = Opcodes.LLOAD;
                }
                default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
            }
        } else if (elementTypeDef.builtin() instanceof FloatType f) {
            switch (f.bits) {
                case 32 -> {
                    newOpcodeArg = Opcodes.T_FLOAT;
                    storeOpcode = Opcodes.FASTORE;
                    loadOpcode = Opcodes.FALOAD;
                    varStoreOpcode = Opcodes.FSTORE;
                    varLoadOpcode = Opcodes.FLOAD;
                }
                case 64 -> {
                    dupOpcode = Opcodes.DUP2_X2;
                    newOpcodeArg = Opcodes.T_DOUBLE;
                    storeOpcode = Opcodes.DASTORE;
                    loadOpcode = Opcodes.DALOAD;
                    varStoreOpcode = Opcodes.DSTORE;
                    varLoadOpcode = Opcodes.DLOAD;
                }
                default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
            }
        } else if (elementTypeDef.builtin() == BoolType.INSTANCE) {
            newOpcodeArg = Opcodes.T_BOOLEAN;
            storeOpcode = Opcodes.BASTORE;
            loadOpcode = Opcodes.BALOAD;
            varStoreOpcode = Opcodes.ISTORE;
            varLoadOpcode = Opcodes.ILOAD;
        } else if (elementTypeDef.isReferenceType() || elementTypeDef.isOptionalReferenceType()) {
            //For now, other types are reference types (or Option<ReferenceType>)
            isReference = true;
            storeOpcode = Opcodes.AASTORE;
            loadOpcode = Opcodes.AALOAD;
            varStoreOpcode = Opcodes.ASTORE;
            varLoadOpcode = Opcodes.ALOAD;
        } else {
            throw new IllegalStateException("None of the cases for ArrayType opcodes matched? Bug in compiler, please report!");
        }
        return new ArrayOpcodes(isReference, newOpcodeArg, dupOpcode, storeOpcode, loadOpcode, varStoreOpcode, varLoadOpcode);
    }

    @Override
    public String name() {
        return "Array";
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return ListUtils.map(generics.get(0).getDescriptor(), d -> "[" + d);
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return "[" + generics.get(0).getReturnTypeDescriptor();
    }

    @Override
    public boolean shouldGenerateStructClassAtRuntime(TypeChecker checker, List<TypeDef> generics) {
        return isPlural(checker, generics);
    }

    @Override
    public boolean isReferenceType(TypeChecker checker, List<TypeDef> generics) {
        return !generics.get(0).isPlural();
    }

    @Override
    public boolean isPlural(TypeChecker checker, List<TypeDef> generics) {
        return generics.get(0).isPlural();
    }

    @Override
    public boolean extensible(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public boolean hasSpecialConstructor(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        if (generics.get(0).isPlural())
            return flattenedElems(generics.get(0)).size();
        return 1;
    }

    private List<TypeDef> flattenedElems(TypeDef typeDef) {
        if (!typeDef.isPlural())
            return List.of(typeDef);
        return ListUtils.join(ListUtils.map(ListUtils.filter(
                typeDef.fields(),
                f -> !f.isStatic()),
                f -> flattenedElems(f.type())));
    }

    private boolean containsNonOptionalReferenceType(TypeDef def) {
        if (def.isPlural()) {
            boolean res = false;
            for (FieldDef fieldDef : def.fields()) {
                if (fieldDef.isStatic()) continue;
                res |= containsNonOptionalReferenceType(fieldDef.type());
            }
            return res;
        } else {
            return def.isReferenceType();
        }
    }


    @Override
    public int numGenerics() {
        return 1;
    }

    @Override
    public TypeDef getInheritanceSupertype(TypeChecker checker, List<TypeDef> generics) {
        return checker.getBasicBuiltin(ObjType.INSTANCE);
    }
}
