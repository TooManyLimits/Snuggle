package builtin_types.types;

import builtin_types.BuiltinType;
import org.objectweb.asm.Type;
import runtime.Unit;

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
    public String getDescriptor(int index) {
        return "L" + Type.getInternalName(Unit.class) + ";";
    }
}
