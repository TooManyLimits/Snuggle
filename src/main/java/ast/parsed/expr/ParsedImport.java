package ast.parsed.expr;

import exceptions.compile_time.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedImport;
import lexing.Loc;
import util.Mutable;

//Whether this is top-level is filled in by the parser
public record ParsedImport(Loc loc, String fileName, Mutable<Boolean> isTopLevel) implements ParsedExpr {

    public ParsedImport(Loc loc, String fileName) {
        this(loc, fileName, new Mutable<>(false));
    }

    @Override
    public TypeResolvedImport resolve(TypeResolver resolver) throws CompilationException {
        if (!isTopLevel.v)
            resolver.doImport(this, false); //If it was top-level, it was already imported
        return new TypeResolvedImport(loc, fileName);
    }
}
