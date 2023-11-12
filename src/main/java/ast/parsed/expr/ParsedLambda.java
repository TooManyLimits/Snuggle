package ast.parsed.expr;

import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedLambda;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;

public record ParsedLambda(Loc loc, List<String> paramNames, ParsedExpr body) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedLambda(loc, paramNames, body.resolve(resolver));
    }

}
