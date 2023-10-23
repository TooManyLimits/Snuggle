package ast.parsed.def.field;

import ast.parsed.ParsedType;
import ast.parsed.expr.ParsedExpr;
import ast.passes.TypeResolver;
import ast.type_resolved.def.field.SnuggleTypeResolvedFieldDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

//Note: initializer is nullable!
public record SnuggleParsedFieldDef(Loc loc, boolean pub, boolean isStatic, String name, ParsedType annotatedType, ParsedExpr initializer) implements ParsedFieldDef {
    @Override
    public SnuggleTypeResolvedFieldDef resolve(TypeResolver resolver) throws CompilationException {
        return new SnuggleTypeResolvedFieldDef(loc, pub, isStatic, name, annotatedType.resolve(loc, resolver), initializer.resolve(resolver));
    }
}
