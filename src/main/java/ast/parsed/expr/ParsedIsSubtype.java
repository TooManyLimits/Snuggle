package ast.parsed.expr;

import ast.parsed.ParsedType;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedIsSubtype;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record ParsedIsSubtype(Loc loc, ParsedType type1, ParsedType type2) implements ParsedExpr {
    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedIsSubtype(loc, type1.resolve(loc, resolver), type2.resolve(loc, resolver));
    }
}
