package ast.parsed.expr;

import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.TypeResolvedFieldAccess;
import ast.type_resolved.expr.TypeResolvedStaticFieldAccess;
import ast.type_resolved.expr.TypeResolvedStaticMethodCall;
import exceptions.compile_time.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import lexing.Loc;
import util.ListUtils;

public record ParsedFieldAccess(Loc loc, ParsedExpr lhs, String name) implements ParsedExpr {
    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        if (lhs instanceof ParsedVariable variable) {
            ResolvedType maybeStaticReceiver = resolver.tryGetBasicType(variable.name());
            if (maybeStaticReceiver != null) {
                //Static field!
                return new TypeResolvedStaticFieldAccess(loc, maybeStaticReceiver, name);
            }
        }
        //Otherwise, regular field
        return new TypeResolvedFieldAccess(loc, lhs.resolve(resolver), name);
    }
}
