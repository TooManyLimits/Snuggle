package builtin_types;

import builtin_types.reflect.Reflector;
import builtin_types.snuggle.always.BoxType;
import builtin_types.snuggle.always.ListType;
import builtin_types.snuggle.SnuggleDefinedType;
import builtin_types.snuggle.extra.ComplexType;
import builtin_types.types.*;
import builtin_types.types.numbers.FloatLiteralType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntLiteralType;
import builtin_types.types.numbers.IntegerType;
import builtin_types.types.reflected.SystemType;

import java.util.*;

/**
 * Contains a set of builtin topLevelTypes which will be accessible from
 * a program. Pass an instance of this, with the appropriate topLevelTypes,
 * to the TypeResolver stage of compilation.
 */
public class BuiltinTypes {

    private final Set<BuiltinType> registeredTypes = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<SnuggleDefinedType> registeredSnuggleTypes = Collections.newSetFromMap(new IdentityHashMap<>());

    public BuiltinTypes() {
        //By default, adds in all the standard topLevelTypes.
        addStandardTypes();
    }

    public BuiltinTypes addType(BuiltinType builtinType) {
        registeredTypes.add(builtinType);
        return this;
    }

    public BuiltinTypes addType(SnuggleDefinedType snuggleDefined) {
        registeredSnuggleTypes.add(snuggleDefined);
        return this;
    }

    public BuiltinTypes addType(Class<?> clazz) {
        return addType(Reflector.reflect(clazz));
    }

    public BuiltinTypes removeType(BuiltinType builtinType) {
        registeredTypes.remove(builtinType);
        return this;
    }

    public BuiltinTypes removeType(SnuggleDefinedType builtinType) {
        registeredSnuggleTypes.remove(builtinType);
        return this;
    }

    public BuiltinTypes removeType(Class<?> clazz) {
        registeredTypes.remove(Reflector.reflect(clazz));
        return this;
    }

    public BuiltinTypes addStandardTypes() {
        //Add snuggle-defined builtins:
        //Always in:
        addType(ListType.INSTANCE);
        addType(BoxType.INSTANCE);

        //Extra:
        addType(ComplexType.INSTANCE);

        //Add java-defined builtins:
        addType(SystemType.class);

        //"Native" builtins
        addType(ExtensionMethods.INSTANCE); //Special, container for extension methods

        addType(OptionType.INSTANCE);
        addType(ArrayType.INSTANCE);
        addType(StringType.INSTANCE);
        addType(MaybeUninit.INSTANCE);

        addType(ObjType.INSTANCE);
        addType(BoolType.INSTANCE);

        addType(IntLiteralType.INSTANCE);
        addType(IntegerType.I8);
        addType(IntegerType.U8);
        addType(IntegerType.I16);
        addType(IntegerType.U16);
        addType(IntegerType.I32);
        addType(IntegerType.U32);
        addType(IntegerType.I64);
        addType(IntegerType.U64);

        addType(FloatLiteralType.INSTANCE);
        addType(FloatType.F32);
        addType(FloatType.F64);
        return this;
    }

    public Collection<BuiltinType> getBuiltins() {
        return registeredTypes;
    }

    public Collection<SnuggleDefinedType> getSnuggleDefined() {
        return registeredSnuggleTypes;
    }

}
