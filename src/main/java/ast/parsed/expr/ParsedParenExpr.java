package ast.parsed.expr;

import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.beans.Expression;

public record ParsedParenExpr(Loc loc, ParsedExpr inside) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return inside.resolve(resolver);
    }
}
