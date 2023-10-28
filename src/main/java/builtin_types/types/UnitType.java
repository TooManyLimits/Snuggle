package builtin_types.types;

import ast.passes.TypeChecker;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import org.objectweb.asm.Type;
import runtime.Unit;

import java.util.List;

public class UnitType implements BuiltinType {

    public static final UnitType INSTANCE = new UnitType();
    private UnitType() {}

    @Override
    public String name() {
        //TODO: Fix unit
        //return "()";
        return "unit";
    }

    @Override
    public boolean nameable() {
        //TODO: Fix unit
        //return false;
        return true;
    }

    @Override
    public String runtimeName(TypeChecker checker, List<TypeDef> generics) {
        return Type.getInternalName(Unit.class);
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return List.of(Type.getDescriptor(Unit.class));
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return Type.getDescriptor(Unit.class);
    }

    @Override
    public boolean isReferenceType(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public boolean isPlural(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public boolean extensible(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        return 1;
    }


}
