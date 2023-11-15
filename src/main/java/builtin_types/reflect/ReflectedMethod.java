package builtin_types.reflect;

import ast.passes.TypeChecker;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.reflect.annotations.*;
import builtin_types.types.*;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import ast.ir.helper.BytecodeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.ListUtils;
import util.throwing_interfaces.ThrowingConsumer;
import util.throwing_interfaces.ThrowingTriFunction;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class ReflectedMethod {

    private final String origName, name, owner, descriptor;
    private final boolean isStatic, isVoid;
    private final List<ThrowingTriFunction<TypeChecker, Loc, TypeDef.InstantiationStackFrame, TypeDef, RuntimeException>> paramTypeGetters;
    private final ThrowingTriFunction<TypeChecker, Loc, TypeDef.InstantiationStackFrame, TypeDef, RuntimeException> ownerTypeGetter, returnTypeGetter;
    private final ThrowingConsumer<MethodVisitor, CompilationException> bytecode;

    ReflectedMethod(Method method) {
        isStatic = Modifier.isStatic(method.getModifiers());
        isVoid = method.getReturnType() == void.class;

        origName = method.getName();
        Rename rename = method.getAnnotation(Rename.class);
        name = rename == null ? origName : rename.value();
        owner = org.objectweb.asm.Type.getInternalName(method.getDeclaringClass());
        descriptor = org.objectweb.asm.Type.getMethodDescriptor(method);

        paramTypeGetters = ListUtils.map(List.of(method.getAnnotatedParameterTypes()), ReflectedMethod::getTypeGetter);
        ownerTypeGetter = (checker, loc, cause) -> checker.getReflectedBuiltin(method.getDeclaringClass());
        returnTypeGetter = getTypeGetter(method.getAnnotatedReturnType());

        bytecode = getBytecode();
    }

    public MethodDef get(TypeChecker checker, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        return new BytecodeMethodDef(
                name,
                isStatic,
                ownerTypeGetter.apply(checker, instantiationLoc, cause),
                ListUtils.map(paramTypeGetters, g -> g.apply(checker, instantiationLoc, cause)),
                returnTypeGetter.apply(checker, instantiationLoc, cause),
                false,
                bytecode
        );
    }

    private ThrowingConsumer<MethodVisitor, CompilationException> getBytecode() {
        //Just call the method in question:
        return v -> v.visitMethodInsn(
                isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                owner,
                origName,
                descriptor,
                false
        );
    }

    private static ThrowingTriFunction<TypeChecker, Loc, TypeDef.InstantiationStackFrame, TypeDef, RuntimeException> getTypeGetter(AnnotatedType type) {
        Class<?> c = (Class<?>) type.getType();
        String className = c.getName().replace('.', '/');

        //Array topLevelTypes
        if (type instanceof AnnotatedArrayType arrayType) {
            ThrowingTriFunction<TypeChecker, Loc, TypeDef.InstantiationStackFrame, TypeDef, RuntimeException> inner = getTypeGetter(arrayType.getAnnotatedGenericComponentType());
            return (checker, loc, cause) -> checker.getGenericBuiltin(ArrayType.INSTANCE, List.of(inner.apply(checker, loc, cause)), loc, cause);
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

    private static ThrowingTriFunction<TypeChecker, Loc, TypeDef.InstantiationStackFrame, TypeDef, RuntimeException> basicBuiltin(BuiltinType b) {
        return (checker, loc, cause) -> checker.getBasicBuiltin(b);
    }

}
