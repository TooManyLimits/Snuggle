package builtin_types.types.numbers;

import ast.passes.TypePool;
import ast.typed.Type;
import builtin_types.BuiltinType;

import java.util.List;

public class FloatType implements BuiltinType {

    public final int bits;
    public final String name;
    public final String descriptor;

    private FloatType(int bits) {
        this.bits = bits;
        this.name = "f" + bits;
        descriptor = switch (bits) {
            case 32 -> "F";
            case 64 -> "D";
            default -> throw new IllegalStateException("Illegal float bits?????? bug in compiler, please report!");
        };
    }

    public static final FloatType F32 = new FloatType(32);
    public static final FloatType F64 = new FloatType(64);

    public static final List<FloatType> ALL_FLOAT_TYPES = List.of(F32, F64);

    @Override
    public String name() {
        return name;
    }

    @Override
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return descriptor;
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
