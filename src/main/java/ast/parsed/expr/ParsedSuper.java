package ast.parsed.expr;

import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import exceptions.CompilationException;
import lexing.Loc;

public record ParsedSuper(Loc loc, int depth) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        throw new IllegalStateException("Shouldn't try to resolve ParsedSuper? Bug in compiler, please report!");
    }
}
