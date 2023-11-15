package builtin_types.reflect;

import ast.passes.TypeChecker;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.reflect.annotations.Unsigned;
import builtin_types.types.ArrayType;
import builtin_types.types.BoolType;
import builtin_types.types.ObjType;
import builtin_types.types.StringType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import lexing.Loc;
import util.throwing_interfaces.ThrowingTriFunction;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.util.List;

public class ReflectionUtil {


    //Get a type getter for a given AnnotatedType
    public static TypeGetter getTypeGetter(AnnotatedType type) {
        Class<?> c = (Class<?>) type.getType();
        String className = c.getName().replace('.', '/');

        //Array topLevelTypes
        if (type instanceof AnnotatedArrayType arrayType) {
            TypeGetter inner = getTypeGetter(arrayType.getAnnotatedGenericComponentType());
            return (checker, loc, cause) -> checker.getGenericBuiltin(ArrayType.INSTANCE, List.of(inner.get(checker, loc, cause)), loc, cause);
        }

        return switch (className) {
            //Primitives
            case "boolean" -> basicBuiltin(BoolType.INSTANCE);
            case "byte" -> {
                if (type.getAnnotation(Unsigned.class) != null)
                    throw new IllegalStateException("Failed to reflect class: Do not use @Unsigned byte, instead use @Unsigned(8) int!");
                yield basicBuiltin(IntegerType.I8);
            }
            case "short" -> {
                if (type.getAnnotation(Unsigned.class) != null)
                    throw new IllegalStateException("Failed to reflect class: Do not use @Unsigned short, instead use @Unsigned(16) int!");
                yield basicBuiltin(IntegerType.I16);
            }
            case "int" -> {
                Unsigned u = type.getAnnotation(Unsigned.class);
                if (u == null)
                    yield basicBuiltin(IntegerType.I32);
                yield switch (u.value()) {
                    case 8 -> basicBuiltin(IntegerType.U8);
                    case 16 -> basicBuiltin(IntegerType.U16);
                    case 0, 32 -> basicBuiltin(IntegerType.U32); //default value is 0, @Unsigned int should be u32
                    default -> throw new IllegalStateException("Failed to reflect class: for \"@Unsigned(n) int\", n should be 0, 8, 16, or 32!");
                };
            }
            case "long" -> {
                Unsigned u = type.getAnnotation(Unsigned.class);
                if (u == null)
                    yield basicBuiltin(IntegerType.I64);
                yield switch (u.value()) {
                    case 0, 64 -> basicBuiltin(IntegerType.U64);
                    default -> throw new IllegalStateException("Failed to reflect class: for \"@Unsigned(n) long\", n should be 0 or 64!");
                };
            }
            case "float" -> basicBuiltin(FloatType.F32);
            case "double" -> basicBuiltin(FloatType.F64);
            case "char" -> throw new IllegalArgumentException("Cannot reflect methods accepting char");
            case "void" -> (checker, loc, cause) -> checker.getTuple(List.of()); //void becomes unit
            //Builtin objects
            case "java/lang/String" -> basicBuiltin(StringType.INSTANCE);
            case "java/lang/Object" -> basicBuiltin(ObjType.INSTANCE);
            //Default
            default -> (checker, loc, cause) -> checker.getReflectedBuiltin(c);
        };
    }

    private static TypeGetter basicBuiltin(BuiltinType b) {
        return (checker, loc, cause) -> checker.getBasicBuiltin(b);
    }

    public interface TypeGetter {
        TypeDef get(TypeChecker checker, Loc loc, TypeDef.InstantiationStackFrame cause);
    }

}
