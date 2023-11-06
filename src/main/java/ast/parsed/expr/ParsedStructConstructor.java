package ast.parsed.expr;

import ast.parsed.ParsedType;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedStructConstructor;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

//If argKeys are unspecified, then the list is null.
public record ParsedStructConstructor(Loc loc, ParsedType parsedType, List<String> argKeys, List<ParsedExpr> argValues) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedStructConstructor(
                loc,
                parsedType == null ? null : parsedType.resolve(loc, resolver),
                argKeys,
                ListUtils.map(argValues, v -> v.resolve(resolver))
        );
    }
}
