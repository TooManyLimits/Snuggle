package ast.parsed.expr;

import ast.type_resolved.expr.TypeResolvedBlock;
import exceptions.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

public record ParsedBlock(Loc loc, List<ParsedExpr> exprs) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedBlock(loc, ListUtils.map(exprs, e -> e.resolve(resolver)));
    }
}
