package builtin_types.reflect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SnuggleType {
    String name() default ""; //The name of the type. Default is the name of the class
    boolean nameable() default true; //Whether this type should be nameable
    String descriptor() default ""; //The descriptor of this type. If none is specified, will try to auto-calculate.
    boolean forceSupertype() default false; //If false, will try to infer the supertype.
    Class<?> supertype() default Object.class; //The supertype of this type, if forceSupertype is true.
    TypeType type() default TypeType.CLASS; //What sort of type this is

    enum TypeType {
        CLASS,
        //STRUCT,
        //ENUM,
        //PRIMITIVE,
    }
}
