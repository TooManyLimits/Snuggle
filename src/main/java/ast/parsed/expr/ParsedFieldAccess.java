package ast.parsed.expr;

import ast.type_resolved.expr.TypeResolvedFieldAccess;
import exceptions.compile_time.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;

public record ParsedFieldAccess(Loc loc, ParsedExpr lhs, String name) implements ParsedExpr {
    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedFieldAccess(loc, lhs.resolve(resolver), name);
    }
}
