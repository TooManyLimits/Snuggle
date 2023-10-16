package builtin_types.reflect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When placed on a byte, short, int, or long field, indicates that
 * the field should be unsigned on the Snuggle side.
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Unsigned {

}
