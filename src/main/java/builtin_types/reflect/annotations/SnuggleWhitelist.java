package builtin_types.reflect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signifies to the Reflector that this method/field should be
 * made accessible to Snuggle programs.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface SnuggleWhitelist {
    //Allow methods with same snuggle-side name and param types, even though
    //java doesn't allow it
    String rename() default "";
}
