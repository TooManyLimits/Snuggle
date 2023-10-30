package ast.typed.def.method;

import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedMethodCall;
import ast.typed.expr.TypedStaticMethodCall;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import util.ThrowingFunction;

import java.util.List;

public record ConstMethodDef(String name, int numGenerics, boolean isStatic, List<TypeDef> paramTypes, TypeDef returnType,
                             ThrowingFunction<TypedMethodCall, TypedExpr, RuntimeException> doConst,
                             ThrowingFunction<TypedStaticMethodCall, TypedExpr, RuntimeException> doConstStatic) implements MethodDef {

    @Override
    public TypedExpr constantFold(TypedMethodCall call) {
        return doConst.apply(call);
    }

    @Override
    public TypedExpr constantFold(TypedStaticMethodCall call) {
        return doConstStatic.apply(call);
    }

    @Override
    public void checkCode() throws CompilationException {
        throw new IllegalStateException("Cannot check code of ConstMethodDef - bug in compiler, please report!");
    }

    @Override
    public TypeDef owningType() {
        throw new IllegalStateException("Should not be asking for owning type of const method def - Bug in compiler, please report!");
    }

    @Override
    public void compileCall(boolean isSuperCall, MethodVisitor jvm) {
        throw new IllegalStateException("Cannot compile call to ConstMethodDef - bug in compiler, please report!");
    }
}
