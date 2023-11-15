package builtin_types.reflect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When placed on an int or long field, indicates that
 * the field should be unsigned on the Snuggle side.
 * The value is the number of bits. By default,
 * u8  = @Unsigned(8) int
 * u16 = @Unsigned(16) int
 * u32 = @Unsigned int
 * u64 = @Unsigned long
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Unsigned {
    int value() default 0;
}
