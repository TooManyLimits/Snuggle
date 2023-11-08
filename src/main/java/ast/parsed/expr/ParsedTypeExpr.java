package ast.parsed.expr;

import ast.parsed.ParsedType;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

/**
 * Only used as an LHS for static method/field accesses,
 * similar to ParsedSuper.
 */
public record ParsedTypeExpr(Loc loc, ParsedType type) implements ParsedExpr {
    
    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        throw new IllegalStateException("Shouldn't try to resolve ParsedTypeExpr? Bug in compiler, please report!");
    }

}
