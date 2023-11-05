package ast.parsed.def.type;

import ast.passes.TypeResolver;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import exceptions.compile_time.CompilationException;

public interface ParsedTypeDef {

    //Get the methodName
    String name();

    //Whether this annotatedType is pub
    boolean pub();

    TypeResolvedTypeDef resolve(TypeResolver resolver) throws CompilationException;

}
