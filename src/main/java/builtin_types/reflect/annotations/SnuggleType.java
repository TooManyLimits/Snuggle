package builtin_types.reflect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SnuggleType {
    TypeType value() default TypeType.CLASS; //What sort of type this is

    enum TypeType {
        CLASS,
        //STRUCT,
        //ENUM,
        //PRIMITIVE,
    }
}
