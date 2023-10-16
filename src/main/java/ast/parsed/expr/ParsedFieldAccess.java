package ast.parsed.expr;

import exceptions.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;

public record ParsedFieldAccess(Loc loc, ParsedExpr lhs, String name) implements ParsedExpr {
    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return null;
    }
}
