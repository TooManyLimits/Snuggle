package ast.typed.expr;

import ast.typed.Type;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;

public record TypedCast(Loc loc, TypedExpr lhs, boolean isMaybe, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {

    }
}
