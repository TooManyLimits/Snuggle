package builtin_types.reflect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SnuggleType {
    String name(); //The name of the type
    boolean nameable() default true; //Whether this type should be nameable
    String descriptor() default ""; //The descriptor of this type. If none is specified, will try to auto-calculate.
    Class<?> supertype() default Object.class; //The supertype of this type. Object by default.
    TypeType type() default TypeType.CLASS; //What sort of type this is

    enum TypeType {
        CLASS,
        //STRUCT,
        //ENUM,
        //PRIMITIVE,
    }
}
