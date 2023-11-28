package builtin_types.types;

import ast.ir.helper.BytecodeHelper;
import ast.passes.TypeChecker;
import ast.typed.def.field.BuiltinFieldDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

//Used for data structures, like Array<T> where T is a reference type or contains a reference type
public class MaybeUninit implements BuiltinType {

    public static final MaybeUninit INSTANCE = new MaybeUninit();
    private MaybeUninit() {}

    @Override
    public List<FieldDef> getFields(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef thisType = checker.getGenericBuiltin(INSTANCE, generics, instantiationLoc, cause);
        TypeDef innerType = generics.get(0);
        if (innerType.get().isPlural()) {
            return ListUtils.map(
                    innerType.nonStaticFields(),
                    f -> new BuiltinFieldDef(
                            f.name(),
                            thisType,
                            checker.getGenericBuiltin(INSTANCE, List.of(f.type()), instantiationLoc, cause),
                            false
                    )
            );
        } else {
            if (innerType.isReferenceType())
                return List.of(new BuiltinFieldDef(innerType.name(), thisType, checker.getGenericBuiltin(OptionType.INSTANCE, List.of(innerType), instantiationLoc, cause), false));
            else
                return List.of(new BuiltinFieldDef(innerType.name(), thisType, innerType, false));
        }
    }

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef thisType = checker.getGenericBuiltin(INSTANCE, generics, instantiationLoc, cause);
        TypeDef innerType = generics.get(0);
        TypeDef unitType = checker.getTuple(List.of());
        return List.of(
                //new with no args -> empty
                new BytecodeMethodDef("new", false, thisType, List.of(), unitType, true, v -> {
                    //Push default, inner value
                    BytecodeHelper.pushDefaultValue(v, innerType);
                }),
                //new with arg -> wrap
                new BytecodeMethodDef("new", false, thisType, List.of(innerType), unitType, true, v -> {
                    //Wraps the argument into one of these
                    //No-op! This stuff is unsafe, it literally just changes the type
                }, BytecodeMethodDef.ZERO), //Cost is 0, as a no-op
                new BytecodeMethodDef("get", false, thisType, List.of(), innerType, true, v -> {
                    //No-op! This is unsafe, it literally just changes the type. Worst that can happen though is an NPE from using the output of this
                }, BytecodeMethodDef.ZERO) //Cost is 0, as a no-op
        );
    }

    @Override
    public String name() {
        return "MaybeUninit";
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return generics.get(0).getDescriptor();
    }

    @Override
    public String runtimeName(TypeChecker checker, List<TypeDef> generics) {
        return generics.get(0).runtimeName();
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return generics.get(0).getReturnTypeDescriptor();
    }

    @Override
    public boolean isReferenceType(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public boolean isPlural(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public boolean hasSpecialConstructor(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public int numGenerics() {
        return 1;
    }

    @Override
    public boolean extensible(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        return generics.get(0).stackSlots();
    }


}
