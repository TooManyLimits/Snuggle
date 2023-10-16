package ast.typed.def.method;

import ast.typed.Type;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedMethodCall;
import ast.typed.expr.TypedStaticMethodCall;
import compile.Compiler;
import exceptions.CompilationException;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.function.Consumer;

public record BytecodeMethodDef(boolean isStatic, String name, List<Type> paramTypes, Type returnType, Consumer<MethodVisitor> bytecode) implements MethodDef {

    @Override
    public int numGenerics() {
        return 0;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public TypedExpr doConst(TypedMethodCall typedCall) throws CompilationException {
        return null;
    }

    @Override
    public TypedExpr doConstStatic(TypedStaticMethodCall typedCall) throws CompilationException {
        return null;
    }

    @Override
    public void compileCall(int opcode, Type owner, Compiler compiler, MethodVisitor visitor) throws CompilationException {
        bytecode.accept(visitor);
    }
}
