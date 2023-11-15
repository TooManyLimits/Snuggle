package builtin_types.reflect.reflect2;

import ast.typed.def.field.FieldDef;

import java.lang.reflect.Field;

/**
 * A method on a reflected class
 */
public class ReflectedField {

    //Reflect the field. Return null if the field should not be reflected.
    public static ReflectedField of(Field field) {
        return null;
    }

}
