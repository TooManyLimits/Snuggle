package ast.parsed.expr;

import exceptions.compile_time.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedImport;
import lexing.Loc;

public record ParsedImport(Loc loc, String fileName) implements ParsedExpr {

    @Override
    public TypeResolvedImport resolve(TypeResolver resolver) throws CompilationException {
        resolver.doImport(this, false);
        return new TypeResolvedImport(loc, fileName);
    }
}
