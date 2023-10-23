package ast.parsed.expr;

import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedWhile;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record ParsedWhile(Loc loc, ParsedExpr cond, ParsedExpr body) implements ParsedExpr {
    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedWhile(loc, cond.resolve(resolver), body.resolve(resolver));
    }
}
