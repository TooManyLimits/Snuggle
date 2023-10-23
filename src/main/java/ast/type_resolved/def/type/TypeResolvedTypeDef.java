package ast.type_resolved.def.type;

import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.Type;
import ast.typed.def.type.TypeDef;

import java.util.List;

public interface TypeResolvedTypeDef {
    String name();

    //The number of generics this must be instantiated with
    int numGenerics();

    //Throw an exception if anything in here violates the generic counts
    void verifyGenericCounts(GenericVerifier verifier) throws CompilationException;

    /**
     * Instantiate this annotatedType with the given list of generics.
     */
    TypeDef instantiate(int index, TypeChecker checker, List<Type> generics) throws CompilationException;

}
