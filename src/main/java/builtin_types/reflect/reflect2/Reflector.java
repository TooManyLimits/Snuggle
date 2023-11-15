package builtin_types.reflect.reflect2;

import java.util.IdentityHashMap;

public class Reflector {

    private static final IdentityHashMap<Class<?>, ReflectedBuiltin> CACHE = new IdentityHashMap<>();

    public static ReflectedBuiltin reflect(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, Reflector::of);
    }

    private static ReflectedBuiltin of(Class<?> clazz) {
        return null;
    }

}
