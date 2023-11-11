package ast.parsed.expr;

import ast.parsed.def.type.ParsedTypeDef;
import ast.passes.TypeResolver;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedTypeDefExpr;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;

public record ParsedTypeDefExpr(Loc loc, ParsedTypeDef typeDef) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        int index = typeDef.nested() ? resolver.addType(loc, typeDef) : resolver.lookup(typeDef);
        return new TypeResolvedTypeDefExpr(loc, new ResolvedType.Basic(index, List.of()));
    }

}
