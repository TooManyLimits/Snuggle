package builtin_types.types;

import builtin_types.BuiltinType;

public class StringType implements BuiltinType {

    public static final StringType INSTANCE = new StringType();
    private StringType() {}

    @Override
    public String name() {
        return "String";
    }

    @Override
    public String getDescriptor(int index) {
        return "Ljava/lang/String;";
    }

    @Override
    public String getRuntimeName() {
        return "java/lang/String";
    }

    @Override
    public boolean extensible() {
        return false;
    }
}
