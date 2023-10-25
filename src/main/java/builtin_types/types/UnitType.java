package builtin_types.types;

import ast.passes.TypePool;
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
    public String getDescriptor(List<ast.typed.Type> generics, TypePool pool) {
        return "L" + getRuntimeName(generics, pool) + ";";
    }

    @Override
    public String getRuntimeName(List<ast.typed.Type> generics, TypePool pool) {
        return Type.getInternalName(Unit.class);
    }

    @Override
    public boolean extensible() {
        return false;
    }

    @Override
    public boolean isReferenceType(List<ast.typed.Type> generics, TypePool pool) {
        return true;
    }
}
