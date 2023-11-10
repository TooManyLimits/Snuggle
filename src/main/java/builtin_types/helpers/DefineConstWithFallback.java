package builtin_types.helpers;

import ast.ir.def.CodeBlock;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.ConstMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedMethodCall;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import util.ThrowingConsumer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class DefineConstWithFallback {

    /**
     * Prefixes the given name with "n_"
     */
    private static BytecodeMethodDef defineBytecodeBinary(String name, TypeDef owningType, TypeDef argType, TypeDef returnType, ThrowingConsumer<MethodVisitor, CompilationException> bytecode) {
        //The bytecode arithmetic operators are prefixed with n_ like this, so they don't conflict
        //with the const version.
        return new BytecodeMethodDef("n_" + name, false, owningType, List.of(argType), returnType, true, bytecode);
    }

    private static <A, B, T> ConstMethodDef defineConstBinary(String name, BiFunction<A, B, T> func, Function<Object, A> receiverConverter, Function<Object, B> argConverter, TypeDef argType, TypeDef returnType, BytecodeMethodDef fallback) {
        return new ConstMethodDef(name, 0, false, List.of(argType), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral literalReceiver &&
                    call.args().get(0) instanceof TypedLiteral literalArg
            ) {
                //Constant-fold into a literal if possible
                return new TypedLiteral(literalReceiver.cause(), call.loc(), func.apply(receiverConverter.apply(literalReceiver.obj()), argConverter.apply(literalArg.obj())), returnType);
            } else {
                //Otherwise, delegate to the fallback implementation
                return new TypedMethodCall(call.loc(), call.receiver(), fallback, call.args(), returnType);
            }
        }, null, fallback);
    }

    public static <A, B, T> List<MethodDef> defineBinary(String name, BiFunction<A, B, T> func, TypeDef owningType, TypeDef argType, TypeDef returnType, ThrowingConsumer<MethodVisitor, CompilationException> doOperation) {
        BytecodeMethodDef bytecodeVersion = defineBytecodeBinary(name, owningType, argType, returnType, doOperation);
        ConstMethodDef constVersion = defineConstBinary(name, func, x -> (A) x, x -> (B) x, argType, returnType, bytecodeVersion);
        return List.of(bytecodeVersion, constVersion);
    }

    public static <A, B, T> List<MethodDef> defineBinaryWithConverter(String name, BiFunction<A, B, T> func, Function<Object, A> receiverConverter, Function<Object, B> argConverter,TypeDef owningType, TypeDef argType, TypeDef returnType, ThrowingConsumer<MethodVisitor, CompilationException> doOperation) {
        BytecodeMethodDef bytecodeVersion = defineBytecodeBinary(name, owningType, argType, returnType, doOperation);
        ConstMethodDef constVersion = defineConstBinary(name, func, receiverConverter, argConverter, argType, returnType, bytecodeVersion);
        return List.of(bytecodeVersion, constVersion);
    }

    private static BytecodeMethodDef defineBytecodeUnary(String name, TypeDef owningType, TypeDef returnType, ThrowingConsumer<MethodVisitor, CompilationException> bytecode) {
        //The bytecode arithmetic operators are prefixed with n_ like this, so they don't conflict
        //with the const version.
        return new BytecodeMethodDef("n_" + name, false, owningType, List.of(), returnType, true, bytecode);
    }

    private static <A, T> ConstMethodDef defineConstUnary(String name, Function<A, T> func, Function<Object, A> converter, TypeDef returnType, BytecodeMethodDef fallback) {
        return new ConstMethodDef(name, 0, false, List.of(), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral literalReceiver) {
                //Constant-fold into a literal if possible
                return new TypedLiteral(literalReceiver.cause(), call.loc(), func.apply(converter.apply(literalReceiver.obj())), returnType);
            } else {
                //Otherwise, delegate to the fallback implementation
                return new TypedMethodCall(call.loc(), call.receiver(), fallback, call.args(), returnType);
            }
        }, null, fallback);
    }

    public static <A, T> List<MethodDef> defineUnary(String name, Function<A, T> func, TypeDef owningType, TypeDef returnType, ThrowingConsumer<MethodVisitor, CompilationException> doOperation) {
        BytecodeMethodDef bytecodeVersion = defineBytecodeUnary(name, owningType, returnType, doOperation);
        ConstMethodDef constVersion = defineConstUnary(name, func, x -> (A) x, returnType, bytecodeVersion);
        return List.of(bytecodeVersion, constVersion);
    }

    public static <A, T> List<MethodDef> defineUnaryWithConverter(String name, Function<A, T> func, Function<Object, A> converter, TypeDef owningType, TypeDef returnType, ThrowingConsumer<MethodVisitor, CompilationException> doOperation) {
        BytecodeMethodDef bytecodeVersion = defineBytecodeUnary(name, owningType, returnType, doOperation);
        ConstMethodDef constVersion = defineConstUnary(name, func, converter, returnType, bytecodeVersion);
        return List.of(bytecodeVersion, constVersion);
    }




}
