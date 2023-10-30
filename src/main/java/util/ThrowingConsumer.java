package util;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T elem) throws E;
}
