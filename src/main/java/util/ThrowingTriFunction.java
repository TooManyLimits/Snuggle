package util;

@FunctionalInterface
public interface ThrowingTriFunction<A, B, C, R, E extends Throwable> {
    R apply(A a, B b, C c) throws E;
}
