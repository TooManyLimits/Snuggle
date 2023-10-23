package ast.parsed.def.field;

import ast.passes.TypeResolver;
import ast.type_resolved.def.field.TypeResolvedFieldDef;
import exceptions.compile_time.CompilationException;

public interface ParsedFieldDef {

    TypeResolvedFieldDef resolve(TypeResolver resolver) throws CompilationException;

}
