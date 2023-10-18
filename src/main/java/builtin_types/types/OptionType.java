package builtin_types.types;

import ast.passes.TypePool;
import ast.typed.Type;
import builtin_types.BuiltinType;

import java.util.List;

public class OptionType implements BuiltinType {

    public static final OptionType INSTANCE = new OptionType();
    private OptionType() {}

    @Override
    public String name() {
        return "Option";
    }

    @Override
    public String genericName(List<Type> generics, TypePool pool) {
        return generics.get(0).name(pool) + "?";
    }

    @Override
    public int numGenerics() { return 1; }

    @Override
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return null;
    }

    @Override
    public String getRuntimeName(List<Type> generics, TypePool pool) {
        return null;
    }

    @Override
    public boolean extensible() {
        return false;
    }
}
