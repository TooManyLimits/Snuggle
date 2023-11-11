package ast.typed.def.method;

import ast.ir.def.CodeBlock;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedMethodCall;
import ast.typed.expr.TypedStaticMethodCall;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import util.throwing_interfaces.ThrowingFunction;

import java.util.List;

public record ConstMethodDef(String name, int numGenerics, boolean isStatic, List<TypeDef> paramTypes, TypeDef returnType,
                             ThrowingFunction<TypedMethodCall, TypedExpr, RuntimeException> doConst,
                             ThrowingFunction<TypedStaticMethodCall, TypedExpr, RuntimeException> doConstStatic, MethodDef delegate) implements MethodDef {

    @Override
    public TypedExpr constantFold(TypedMethodCall call) {
        return doConst.apply(call);
    }

    @Override
    public TypedExpr constantFold(TypedStaticMethodCall call) {
        return doConstStatic.apply(call);
    }

    @Override
    public MethodDef delegate() {
        if (this.delegate == null)
            throw new IllegalStateException("This ConstMethodDef has no delegate - bug in compiler, please report!");
        return delegate;
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
    public void compileCall(boolean isSuperCall, CodeBlock block, List<FieldDef> desiredFields, MethodVisitor jvm) {
        throw new IllegalStateException("Cannot compile call to ConstMethodDef - bug in compiler, please report!");
    }
}
