package ast.parsed.expr;

import exceptions.compile_time.CompilationException;
import ast.parsed.ParsedType;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedConstructor;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

public record ParsedConstructor(Loc loc, ParsedType parsedType, List<ParsedExpr> args) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedConstructor(loc, parsedType.resolve(loc, resolver), ListUtils.map(args, a -> a.resolve(resolver)));
    }
}
