package ast.type_resolved.def.type;

import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.def.type.TypeDef;
import lexing.Loc;

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
    TypeDef instantiate(TypeDef currentType, TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause);

    static String instantiateName(String name, List<TypeDef> generics) {
        StringBuilder newName = new StringBuilder(name);
        if (generics.size() > 0) {
            newName.append("(");
            for (TypeDef t : generics) {
                newName.append(t.name());
                newName.append(", ");
            }
            newName.delete(newName.length() - 2, newName.length());
            newName.append(")");
        }
        return newName.toString();
    }

}
