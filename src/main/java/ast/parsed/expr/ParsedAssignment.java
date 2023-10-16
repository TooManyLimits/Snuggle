package ast.parsed.expr;

import ast.type_resolved.expr.TypeResolvedAssignment;
import exceptions.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;

public record ParsedAssignment(Loc loc, ParsedExpr lhs, ParsedExpr rhs) implements ParsedExpr {
    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedAssignment(loc, lhs.resolve(resolver), rhs.resolve(resolver));
    }
}
