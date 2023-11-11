package ast.parsed.expr;

import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.TypeResolvedStaticMethodCall;
import ast.type_resolved.expr.TypeResolvedSuperMethodCall;
import exceptions.compile_time.CompilationException;
import ast.parsed.ParsedType;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedMethodCall;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

/**
 * A method call. For example:
 * a.b()
 * a.c(1, 2, d)
 * a / 2 (translated to a.div(2) in ast.passes.Parser)
 */
public record ParsedMethodCall(Loc loc, ParsedExpr receiver, String methodName, List<ParsedType> genericArgs, List<ParsedExpr> args) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        //Test for static!
        if (receiver instanceof ParsedVariable name) {
            //If the receiver is just a methodName...
            ResolvedType maybeStaticReceiver = resolver.tryGetBasicType(name.name());
            if (maybeStaticReceiver != null) {
                //We have a static method call!
                return new TypeResolvedStaticMethodCall(
                        loc,
                        maybeStaticReceiver,
                        methodName,
                        ListUtils.map(genericArgs, g -> g.resolve(loc, resolver)),
                        ListUtils.map(args, a -> a.resolve(resolver))
                );
            }
        } else if (receiver instanceof ParsedTypeExpr typeExpr) {
            //It's static
            return new TypeResolvedStaticMethodCall(
                    loc,
                    typeExpr.type().resolve(typeExpr.loc(), resolver),
                    methodName,
                    ListUtils.map(genericArgs, g -> g.resolve(loc, resolver)),
                    ListUtils.map(args, a -> a.resolve(resolver))
            );
        } else if (receiver instanceof ParsedSuper) {
            //If we're calling a super method, then create a SuperMethodCall.
            return new TypeResolvedSuperMethodCall(
                    loc,
                    methodName,
                    ListUtils.map(genericArgs, g -> g.resolve(loc, resolver)),
                    ListUtils.map(args, a -> a.resolve(resolver))
            );
        }
        //Otherwise, just a normal method call
        return new TypeResolvedMethodCall(
                loc,
                receiver.resolve(resolver),
                methodName,
                null,
                ListUtils.map(genericArgs, g -> g.resolve(loc, resolver)),
                ListUtils.map(args, a -> a.resolve(resolver))
        );
    }

}
