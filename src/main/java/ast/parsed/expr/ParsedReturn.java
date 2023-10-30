package ast.parsed.expr;

import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedReturn;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record ParsedReturn(Loc loc, ParsedExpr rhs) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedReturn(loc, rhs.resolve(resolver));
    }
}
