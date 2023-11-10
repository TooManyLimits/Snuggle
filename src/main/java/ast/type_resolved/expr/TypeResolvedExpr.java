package ast.type_resolved.expr;

import ast.type_resolved.def.field.TypeResolvedFieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.expr.TypedExpr;
import lexing.Loc;

import java.util.List;

public interface TypeResolvedExpr {

    Loc loc();

    //Throw exception if a problem is encountered
    void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException;

    //infer() and check() both must be able to instantiate any generic ResolvedType inside them,
    //which is what the passed typeGenerics are for.

    //Attempt to infer the annotatedType of this expr.
    TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException;

    //Check that the annotatedType of this expr matches the expected annotatedType. If it doesn't, error.
    //
    //IMPORTANT NOTE:
    //If this check() call fails, then ensure that the checker parameter's variables and scopes are NOT MODIFIED.
    //The exception to this is for expressions which cannot exist inside of method parameters
    //(currently only declarations modify the checker)
    TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException;

}
