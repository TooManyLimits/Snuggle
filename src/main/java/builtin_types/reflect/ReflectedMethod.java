package builtin_types.reflect;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import builtin_types.BuiltinTypes;
import builtin_types.reflect.annotations.*;
import builtin_types.types.BoolType;
import builtin_types.types.ObjType;
import builtin_types.types.StringType;
import builtin_types.types.UnitType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import compile.BytecodeHelper;
import exceptions.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.ListUtils;
import util.ThrowingFunction;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReflectedMethod {

    private final String origName, name, owner, descriptor;
    private final boolean inlined, isStatic, isVoid;
    private final List<ThrowingFunction<TypePool, Type, CompilationException>> paramTypeGetters;
    private final ThrowingFunction<TypePool, Type, CompilationException> returnTypeGetter;
    private final Consumer<MethodVisitor> bytecode;

    ReflectedMethod(Method method) {
        inlined = method.getAnnotation(Inline.class) != null;
        isStatic = Modifier.isStatic(method.getModifiers());
        isVoid = method.getReturnType() == void.class;

        origName = method.getName();
        Rename rename = method.getAnnotation(Rename.class);
        name = rename == null ? origName : rename.value();
        owner = org.objectweb.asm.Type.getInternalName(method.getDeclaringClass());
        descriptor = ReflectionUtils.getDescriptor(List.of(method.getParameterTypes()), method.getReturnType());

        paramTypeGetters = ListUtils.map(List.of(method.getAnnotatedParameterTypes()), ReflectedMethod::getTypeGetter);
        returnTypeGetter = getTypeGetter(method.getAnnotatedReturnType());

        bytecode = getBytecode();
    }

    public MethodDef get(TypePool pool) throws CompilationException {
        return new BytecodeMethodDef(
                isStatic,
                name,
                ListUtils.map(paramTypeGetters, g -> g.apply(pool)),
                returnTypeGetter.apply(pool),
                bytecode
        );
    }

    private Consumer<MethodVisitor> getBytecode() {
        if (inlined) {
            //special thingis
            throw new IllegalArgumentException("Inline not available");
        } else {
            //Not inline, so just call the method in question:
            return v -> {
                v.visitMethodInsn(
                        isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                        owner,
                        origName,
                        descriptor,
                        false
                );
                //If the return type is void, then we have to push unit
                if (isVoid) BytecodeHelper.pushUnit(v);
            };
        }
    }

    private static ThrowingFunction<TypePool, Type, CompilationException> getTypeGetter(AnnotatedType type) {
        Class<?> c = (Class<?>) type.getType();
        String className = c.getName().replace('.', '/');
        return switch (className) {
            //Primitives
            case "boolean" -> basicBuiltin(BoolType.INSTANCE);
            case "byte" -> maybeUnsigned(type, IntegerType.I8, IntegerType.U8);
            case "short" -> maybeUnsigned(type, IntegerType.I16, IntegerType.U16);
            case "int" -> maybeUnsigned(type, IntegerType.I32, IntegerType.U32);
            case "long" -> maybeUnsigned(type, IntegerType.I64, IntegerType.U64);
            case "float" -> basicBuiltin(FloatType.F32);
            case "double" -> basicBuiltin(FloatType.F64);
            case "char" -> throw new IllegalArgumentException("Cannot reflect methods accepting char");
            case "void" -> basicBuiltin(UnitType.INSTANCE); //void becomes unit
            //Builtin objects
            case "java/lang/String" -> basicBuiltin(StringType.INSTANCE);
            case "java/lang/Object" -> basicBuiltin(ObjType.INSTANCE);
            //Default
            default -> pool -> pool.getReflectedBuiltin(c);
        };
    }

    private static ThrowingFunction<TypePool, Type, CompilationException> maybeUnsigned(AnnotatedType t, BuiltinType signed, BuiltinType unsigned) {
        return t.getAnnotation(Unsigned.class) != null ? pool -> pool.getBasicBuiltin(unsigned) : pool -> pool.getBasicBuiltin(signed);
    }

    private static ThrowingFunction<TypePool, Type, CompilationException> basicBuiltin(BuiltinType b) {
        return pool -> pool.getBasicBuiltin(b);
    }

}
