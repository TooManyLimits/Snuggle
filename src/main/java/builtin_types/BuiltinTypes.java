package builtin_types;

import builtin_types.reflect.ReflectedBuiltin;
import builtin_types.types.*;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntLiteralType;
import builtin_types.types.numbers.IntegerType;
import builtin_types.types.reflected.SystemType;

import java.util.*;

/**
 * Contains a set of builtin types which will be accessible from
 * a program. Pass an instance of this, with the appropriate types,
 * to the TypeResolver stage of compilation.
 */
public class BuiltinTypes {

    //Because no IdentityHashSet exists
    private final IdentityHashMap<BuiltinType, Void> registeredTypes = new IdentityHashMap<>();

    public BuiltinTypes() {
        //By default, adds in all the standard types.
        addStandardTypes();
    }

    public BuiltinTypes addType(BuiltinType builtinType) {
        registeredTypes.put(builtinType, null);
        return this;
    }

    public BuiltinTypes reflect(Class<?> clazz) {
        return addType(new ReflectedBuiltin(clazz));
    }

    public BuiltinTypes removeType(BuiltinType builtinType) {
        registeredTypes.remove(builtinType);
        return this;
    }

    public BuiltinTypes addStandardTypes() {
        //addType(SystemType.INSTANCE);
        addType(SystemType.INSTANCE);

        addType(OptionType.INSTANCE);

        addType(ObjType.INSTANCE);
        addType(UnitType.INSTANCE);
        addType(BoolType.INSTANCE);
        addType(StringType.INSTANCE);

        addType(IntLiteralType.INSTANCE);
        addType(IntegerType.I8);
        addType(IntegerType.U8);
        addType(IntegerType.I16);
        addType(IntegerType.U16);
        addType(IntegerType.I32);
        addType(IntegerType.U32);
        addType(IntegerType.I64);
        addType(IntegerType.U64);

        addType(FloatType.F32);
        addType(FloatType.F64);
        return this;
    }

    public Collection<BuiltinType> getAll() {
        return registeredTypes.keySet();
    }

}
