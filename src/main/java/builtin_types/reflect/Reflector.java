package builtin_types.reflect;

import builtin_types.reflect.annotations.SnuggleType;

import java.util.IdentityHashMap;

public class Reflector {

    private static final IdentityHashMap<Class<?>, ReflectedBuiltin> CACHE = new IdentityHashMap<>();

    public static ReflectedBuiltin reflect(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, Reflector::of);
    }

    private static ReflectedBuiltin of(Class<?> clazz) {
        if (clazz.isAnnotationPresent(SnuggleType.class)) {
            return switch (clazz.getAnnotation(SnuggleType.class).value()) {
                case CLASS -> new ReflectedClass(clazz);
            };
        } else {
            throw new IllegalArgumentException("Cannot reflect class \"" + clazz + "\", as it has no @SnuggleType annotation!");
        }
    }

}
