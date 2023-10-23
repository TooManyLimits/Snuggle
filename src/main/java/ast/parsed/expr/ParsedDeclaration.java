package ast.parsed.expr;

import ast.type_resolved.expr.TypeResolvedDeclaration;
import exceptions.compile_time.CompilationException;
import ast.parsed.ParsedType;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;

public record ParsedDeclaration(Loc loc, String name, ParsedType annotatedType, ParsedExpr rhs) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedDeclaration(
                loc,
                name,
                annotatedType == null ? null : annotatedType.resolve(loc, resolver),
                rhs.resolve(resolver)
        );
    }
}
