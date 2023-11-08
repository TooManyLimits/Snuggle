package ast.parsed.expr;

import ast.passes.TypeResolver;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedFieldAccess;
import ast.type_resolved.expr.TypeResolvedStaticFieldAccess;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record ParsedFieldAccess(Loc loc, ParsedExpr lhs, String name) implements ParsedExpr {
    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        if (lhs instanceof ParsedVariable variable) {
            ResolvedType maybeStaticReceiver = resolver.tryGetBasicType(variable.name());
            if (maybeStaticReceiver != null) {
                //Static field!
                return new TypeResolvedStaticFieldAccess(loc, maybeStaticReceiver, name);
            }
        } else if (lhs instanceof ParsedTypeExpr typeExpr) {
            //It's static
            ResolvedType staticReceiver = typeExpr.type().resolve(typeExpr.loc(), resolver);
            return new TypeResolvedStaticFieldAccess(loc, staticReceiver, name);
        }
        //Otherwise, regular field
        return new TypeResolvedFieldAccess(loc, lhs.resolve(resolver), name, false);
    }
}
