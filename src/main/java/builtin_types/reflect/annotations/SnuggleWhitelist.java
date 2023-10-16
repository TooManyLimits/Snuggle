package builtin_types.reflect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signifies to the Reflector that this method/field should be
 * made accessible to Snuggle programs.
 * If placed on a type, then methods/fields in the class
 * become whitelisted by default, unless marked with
 * @SnuggleBlacklist.
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface SnuggleWhitelist {}
