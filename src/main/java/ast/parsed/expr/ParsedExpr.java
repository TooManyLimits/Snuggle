package ast.parsed.expr;

import exceptions.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;

public interface ParsedExpr {

    Loc loc();

    /**
     * Resolve this ParsedExpr into a TypeResolvedExpr.
     *
     * The main difference between them is that any "ParsedType" objects in the parsed expr
     * are resolved into "ResolvedType" objects instead, using the TypeResolver.
     * We should not have to deal with any more "ParsedType" objects after the
     * resolve() function completes.
     *
     * Throws a CompilationException if:
     * - A referenced annotatedType does not exist in the current context
     */
    TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException;

}
