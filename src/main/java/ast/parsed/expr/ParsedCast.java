package ast.parsed.expr;

import ast.parsed.ParsedType;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedCast;
import ast.type_resolved.expr.TypeResolvedExpr;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record ParsedCast(Loc loc, ParsedExpr lhs, boolean isMaybe, ParsedType type) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedCast(loc, lhs.resolve(resolver), isMaybe, type.resolve(loc, resolver));
    }
}
