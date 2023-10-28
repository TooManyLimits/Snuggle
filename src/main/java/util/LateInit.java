package util;

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
        filled = true;
        cachedResult = getter.get();
        return cachedResult;
    }

    public T getAlreadyFilled() {
        if (filled)
            return cachedResult;
        throw new IllegalStateException("Attempt to getAlreadyFilled() on LateInit, but it was not already filled!");
    }

    @Override
    public String toString() {
        if (filled)
            return "LateInit[" + cachedResult + "]";
        return "LateInit[Empty]";
    }
}
