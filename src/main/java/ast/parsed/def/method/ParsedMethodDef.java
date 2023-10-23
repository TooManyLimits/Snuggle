package ast.parsed.def.method;

import exceptions.compile_time.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.def.method.TypeResolvedMethodDef;

public interface ParsedMethodDef {

    TypeResolvedMethodDef resolve(TypeResolver resolver) throws CompilationException;

}
