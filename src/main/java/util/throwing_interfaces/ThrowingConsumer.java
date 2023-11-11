package util.throwing_interfaces;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T elem) throws E;
}
