package ast.parsed.def.method;

import exceptions.CompilationException;
import ast.parsed.ParsedType;
import ast.parsed.expr.ParsedExpr;
import ast.passes.TypeResolver;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

/**
 * A ParsedMethodDef which was defined in Snuggle code.
 */
public record SnuggleParsedMethodDef(Loc loc, boolean pub, boolean isStatic, String name, int numGenerics, List<String> paramNames, List<ParsedType> paramTypes, ParsedType returnType, ParsedExpr body) implements ParsedMethodDef {


    @Override
    public SnuggleTypeResolvedMethodDef resolve(TypeResolver resolver) throws CompilationException {
        return new SnuggleTypeResolvedMethodDef(
                loc,
                isStatic,
                name,
                numGenerics,
                paramNames,
                ListUtils.map(paramTypes, t -> t.resolve(loc, resolver)),
                returnType.resolve(loc, resolver),
                body.resolve(resolver)
        );
    }

}
