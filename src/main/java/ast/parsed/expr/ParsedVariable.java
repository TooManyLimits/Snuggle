package ast.parsed.expr;

import ast.type_resolved.expr.TypeResolvedVariable;
import exceptions.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;

public record ParsedVariable(Loc loc, String name) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedVariable(loc, name);
    }
}
