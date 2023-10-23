package builtin_types.helpers;

import ast.typed.Type;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.ConstMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedMethodCall;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class DefineConstWithFallback {

    /**
     * Prefixes the given name with "n_"
     */
    private static BytecodeMethodDef defineBytecodeBinary(String name, Type argType, Type returnType, Consumer<MethodVisitor> bytecode) {
        //The bytecode arithmetic operators are prefixed with n_ like this, so they don't conflict
        //with the const version.
        return new BytecodeMethodDef(false, "n_" + name, List.of(argType), returnType, bytecode);
    }

    private static <A, B, T> ConstMethodDef defineConstBinary(String name, BiFunction<A, B, T> func, Function<Object, A> receiverConverter, Function<Object, B> argConverter, Type argType, Type returnType, BytecodeMethodDef fallback) {
        return new ConstMethodDef(name, 0, false, List.of(argType), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral literalReceiver &&
                    call.args().get(0) instanceof TypedLiteral literalArg
            ) {
                //Constant-fold into a literal if possible
                return new TypedLiteral(call.loc(), func.apply(receiverConverter.apply(literalReceiver.obj()), argConverter.apply(literalArg.obj())), returnType);
            } else {
                //Otherwise, delegate to the fallback implementation
                return new TypedMethodCall(call.loc(), call.receiver(), fallback, call.args(), returnType);
            }
        }, null);
    }

    public static <A, B, T> List<MethodDef> defineBinary(String name, BiFunction<A, B, T> func, Type argType, Type returnType, Consumer<MethodVisitor> doOperation) {
        BytecodeMethodDef bytecodeVersion = defineBytecodeBinary(name, argType, returnType, doOperation);
        ConstMethodDef constVersion = defineConstBinary(name, func, x -> (A) x, x -> (B) x, argType, returnType, bytecodeVersion);
        return List.of(bytecodeVersion, constVersion);
    }

    public static <A, B, T> List<MethodDef> defineBinaryWithConverter(String name, BiFunction<A, B, T> func, Function<Object, A> receiverConverter, Function<Object, B> argConverter, Type argType, Type returnType, Consumer<MethodVisitor> doOperation) {
        BytecodeMethodDef bytecodeVersion = defineBytecodeBinary(name, argType, returnType, doOperation);
        ConstMethodDef constVersion = defineConstBinary(name, func, receiverConverter, argConverter, argType, returnType, bytecodeVersion);
        return List.of(bytecodeVersion, constVersion);
    }

    private static BytecodeMethodDef defineBytecodeUnary(String name, Type returnType, Consumer<MethodVisitor> bytecode) {
        //The bytecode arithmetic operators are prefixed with n_ like this, so they don't conflict
        //with the const version.
        return new BytecodeMethodDef(false, "n_" + name, List.of(), returnType, bytecode);
    }

    private static <A, T> ConstMethodDef defineConstUnary(String name, Function<A, T> func, Function<Object, A> converter, Type returnType, BytecodeMethodDef fallback) {
        return new ConstMethodDef(name, 0, false, List.of(), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral literalReceiver) {
                //Constant-fold into a literal if possible
                return new TypedLiteral(call.loc(), func.apply(converter.apply(literalReceiver.obj())), returnType);
            } else {
                //Otherwise, delegate to the fallback implementation
                return new TypedMethodCall(call.loc(), call.receiver(), fallback, call.args(), returnType);
            }
        }, null);
    }

    public static <A, T> List<MethodDef> defineUnary(String name, Function<A, T> func, Type returnType, Consumer<MethodVisitor> doOperation) {
        BytecodeMethodDef bytecodeVersion = defineBytecodeUnary(name, returnType, doOperation);
        ConstMethodDef constVersion = defineConstUnary(name, func, x -> (A) x, returnType, bytecodeVersion);
        return List.of(bytecodeVersion, constVersion);
    }

    public static <A, T> List<MethodDef> defineUnaryWithConverter(String name, Function<A, T> func, Function<Object, A> converter, Type returnType, Consumer<MethodVisitor> doOperation) {
        BytecodeMethodDef bytecodeVersion = defineBytecodeUnary(name, returnType, doOperation);
        ConstMethodDef constVersion = defineConstUnary(name, func, converter, returnType, bytecodeVersion);
        return List.of(bytecodeVersion, constVersion);
    }




}
