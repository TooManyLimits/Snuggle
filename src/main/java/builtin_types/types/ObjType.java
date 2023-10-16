package builtin_types.types;

import builtin_types.BuiltinType;

public class ObjType implements BuiltinType {

    public static final ObjType INSTANCE = new ObjType();
    private ObjType() {}

    @Override
    public String name() {
        return "Obj";
    }

    @Override
    public String getDescriptor(int index) {
        return "Ljava/lang/Object;";
    }
}
