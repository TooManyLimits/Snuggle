package ast.parsed.expr;

import ast.type_resolved.expr.TypeResolvedIf;
import exceptions.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;

public record ParsedIf(Loc loc, ParsedExpr cond, ParsedExpr ifTrue, ParsedExpr ifFalse) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedIf(loc, cond.resolve(resolver), ifTrue.resolve(resolver), ifFalse == null ? null : ifFalse.resolve(resolver));
    }
}
