package builtin_types.types;

import builtin_types.BuiltinType;

public class OptionType implements BuiltinType {

    public static final OptionType INSTANCE = new OptionType();
    private OptionType() {}

    @Override
    public String name() {
        return "Option";
    }

    @Override
    public String getDescriptor(int index) {
        return null;
    }

    @Override
    public String getRuntimeName() {
        return null;
    }

    @Override
    public boolean extensible() {
        return false;
    }
}
