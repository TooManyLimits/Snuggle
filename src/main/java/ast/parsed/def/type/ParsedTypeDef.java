package ast.parsed.def.type;

import ast.passes.TypeResolver;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import exceptions.compile_time.CompilationException;

public interface ParsedTypeDef {

    //Get the methodName
    String name();

    //Whether this annotatedType is pub
    boolean pub();

    //Whether this typedef is inside another one
    //If this is true, then pub() should never be true
    boolean nested();

    TypeResolvedTypeDef resolve(TypeResolver resolver) throws CompilationException;

}
