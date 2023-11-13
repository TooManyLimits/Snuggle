package util.throwing_interfaces;

@FunctionalInterface
public interface ThrowingBiConsumer<T, S, E extends Throwable> {
    void accept(T value, S value2) throws E;
}
