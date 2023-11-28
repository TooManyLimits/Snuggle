package ast.parsed.def.method;

import ast.type_resolved.def.method.TypeResolvedMethodDef;
import exceptions.compile_time.CompilationException;
import ast.parsed.ParsedType;
import ast.parsed.expr.ParsedExpr;
import ast.passes.TypeResolver;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import lexing.Loc;
import util.LateInitFunction;
import util.ListUtils;
import util.Mutable;

import java.util.List;

/**
 * A ParsedMethodDef which was defined in Snuggle code.
 */
public record SnuggleParsedMethodDef(Loc loc, boolean pub, boolean isStatic, String name, int numGenerics, List<String> paramNames, List<ParsedType> paramTypes, ParsedType returnType, ParsedExpr body,
                                     LateInitFunction<TypeResolver, SnuggleTypeResolvedMethodDef, CompilationException> resolveCache) implements ParsedMethodDef {

    public SnuggleParsedMethodDef(Loc loc, boolean pub, boolean isStatic, String name, int numGenerics, List<String> paramNames, List<ParsedType> paramTypes, ParsedType returnType, ParsedExpr body) {
        this(loc, pub, isStatic, name, numGenerics, paramNames, paramTypes, returnType, body,
                new LateInitFunction<>(resolver -> new SnuggleTypeResolvedMethodDef(
                        loc,
                        pub,
                        isStatic,
                        name,
                        numGenerics,
                        paramNames,
                        ListUtils.map(paramTypes, t -> t.resolve(loc, resolver)),
                        returnType.resolve(loc, resolver),
                        body.resolve(resolver)
                )));
    }

    @Override
    public SnuggleTypeResolvedMethodDef resolve(TypeResolver resolver) throws CompilationException {
        return resolveCache.get(resolver);
    }

}
