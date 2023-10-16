package ast.parsed.def.type;

import exceptions.CompilationException;
import ast.passes.TypeResolver;
import ast.type_resolved.def.type.TypeResolvedTypeDef;

public interface ParsedTypeDef {

    //Get the methodName
    String name();

    //Whether this annotatedType is pub
    boolean pub();

    TypeResolvedTypeDef resolve(TypeResolver resolver) throws CompilationException;

}
