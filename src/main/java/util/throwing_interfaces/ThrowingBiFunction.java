package util.throwing_interfaces;

@FunctionalInterface
public interface ThrowingBiFunction<T, S, R, E extends Throwable> {
    R apply(T value, S value2) throws E;
}
