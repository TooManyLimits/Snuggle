package util;

import util.throwing_interfaces.ThrowingSupplier;

import java.util.function.Function;

//Runs the getter on first get() attempt, and caches result
public class LateInit<T, E extends Throwable> {

    private T cachedResult;
    private final ThrowingSupplier<T, E> getter;
    private boolean filled;

    public LateInit(ThrowingSupplier<T, E> getter) {
        this.getter = getter;
    }

    public T get() throws E {
        if (filled)
            return cachedResult;
        cachedResult = getter.get();
        filled = true;
        return cachedResult;
    }

    public <R> R tryGet(Function<T, R> func) {
        try {
            return func.apply(get());
        } catch (Throwable e) {
            return null;
        }
    }


    public T getAlreadyFilled() {
        if (filled)
            return cachedResult;
        throw new IllegalStateException("Attempt to getAlreadyFilled() on LateInit, but it was not already filled! Bug in compiler, please report!");
    }

    @Override
    public String toString() {
        if (filled)
            return "LateInit[" + cachedResult + "]";
        return "LateInit[Empty]";
    }
}
