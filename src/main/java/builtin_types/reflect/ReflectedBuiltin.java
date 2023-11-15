package builtin_types.reflect;

import builtin_types.BuiltinType;

public interface ReflectedBuiltin extends BuiltinType {

    //Get the java class that this was reflected from
    Class<?> getJavaClass();

}
