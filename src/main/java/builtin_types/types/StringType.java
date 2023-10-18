package builtin_types.types;

import ast.passes.TypePool;
import ast.typed.Type;
import builtin_types.BuiltinType;

import java.util.List;

public class StringType implements BuiltinType {

    public static final StringType INSTANCE = new StringType();
    private StringType() {}

    @Override
    public String name() {
        return "String";
    }

    @Override
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return "Ljava/lang/String;";
    }

    @Override
    public String getRuntimeName(List<Type> generics, TypePool pool) {
        return "java/lang/String";
    }

    @Override
    public boolean extensible() {
        return false;
    }
}
