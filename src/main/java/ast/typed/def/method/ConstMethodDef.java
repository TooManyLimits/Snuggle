package ast.typed.def.method;

import ast.typed.expr.TypedStaticMethodCall;
import compile.Compiler;
import exceptions.compile_time.CompilationException;
import ast.typed.Type;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedMethodCall;
import org.objectweb.asm.MethodVisitor;
import util.ThrowingFunction;

import java.util.List;

//ConstMethodDef is created from a lambda, essentially, which
//converts a TypedMethodCall into a TypedExpr.
//This happens at compile time; ConstMethodDef are essentially macros.
public record ConstMethodDef(String name, int numGenerics, boolean isStatic, List<Type> paramTypes, Type returnType, ThrowingFunction<TypedMethodCall, TypedExpr, CompilationException> doConst, ThrowingFunction<TypedStaticMethodCall, TypedExpr, CompilationException> doConstStatic) implements MethodDef {

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public TypedExpr doConst(TypedMethodCall typedCall) throws CompilationException {
        return doConst.apply(typedCall);
    }

    @Override
    public TypedExpr doConstStatic(TypedStaticMethodCall typedCall) throws CompilationException {
        return doConstStatic.apply(typedCall);
    }

    @Override
    public void compileCall(int opcode, Type owner, Compiler compiler, MethodVisitor visitor) throws CompilationException {
        throw new IllegalStateException("Cannot compile call to ConstMethodDef. Bug in compiler, please report!");
    }
}
