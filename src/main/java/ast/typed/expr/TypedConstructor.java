package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public record TypedConstructor(Loc loc, Type type, MethodDef method, List<TypedExpr> args) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        visitor.visitTypeInsn(Opcodes.NEW, compiler.getTypeDef(type).getRuntimeName());
        visitor.visitInsn(Opcodes.DUP);
        for (TypedExpr arg : args)
            arg.compile(compiler, env, visitor);
        method.compileCall(Opcodes.INVOKESPECIAL, type, compiler, visitor);
    }
}
