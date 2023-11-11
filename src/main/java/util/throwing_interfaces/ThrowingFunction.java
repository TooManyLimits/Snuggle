package util.throwing_interfaces;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Throwable> {
    R apply(T value) throws E;
}
