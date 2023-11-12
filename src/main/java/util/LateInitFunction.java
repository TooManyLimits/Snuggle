package util;

import util.throwing_interfaces.ThrowingFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class LateInitFunction<P, T, E extends Throwable> {

    private final Map<P, T> cachedResults = new HashMap<>();
    private final ThrowingFunction<P, T, E> getter;

    public LateInitFunction(ThrowingFunction<P, T, E> getter) {
        this.getter = getter;
    }

    public T get(P param) throws E {
        if (cachedResults.containsKey(param))
            return cachedResults.get(param);
        T result = getter.apply(param);
        cachedResults.put(param, result);
        return result;
    }

    public <R> R tryGet(P param, Function<T, R> func) {
        try {
            return func.apply(get(param));
        } catch (Throwable e) {
            return null;
        }
    }

    public T getAlreadyFilled(P param) {
        if (cachedResults.containsKey(param))
            return cachedResults.get(param);
        throw new IllegalStateException("Attempt to getAlreadyFilled() on LateInitFunction, but it was not already filled! Bug in compiler, please report!");
    }

    @Override
    public String toString() {
        return "LateInitFunction[" + cachedResults + "]";
    }


}
