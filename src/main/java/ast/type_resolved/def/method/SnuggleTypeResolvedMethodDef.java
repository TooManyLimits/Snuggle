package ast.type_resolved.def.method;

import ast.typed.def.method.SnuggleMethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.typed.expr.TypedExpr;
import lexing.Loc;
import util.LateInit;
import util.ListUtils;

import java.util.List;

public record SnuggleTypeResolvedMethodDef(Loc loc, boolean pub, boolean isStatic, String name, int numGenerics, List<String> paramNames, List<ResolvedType> paramTypes, ResolvedType returnType, TypeResolvedExpr body) implements TypeResolvedMethodDef {

    //Same as in other types. Error if there's a violation.
    public void verifyGenericCounts(GenericVerifier verifier) throws CompilationException {
        //Verify return annotatedType
        verifier.verifyType(returnType, loc);
        //Verify param types
        for (ResolvedType t : paramTypes)
            verifier.verifyType(t, loc);
        //Verify body
        body.verifyGenericArgCounts(verifier);
    }

    //allMethods are passed in for disambiguating between method names.
    public SnuggleMethodDef instantiateType(List<? extends TypeResolvedMethodDef> allMethods, TypeDef currentType, TypeChecker checker, List<TypeDef> generics) {
        List<TypeDef> newParamTypes = ListUtils.map(paramTypes, t -> checker.getOrInstantiate(t, generics));
        TypeDef newReturnType = checker.getOrInstantiate(returnType, generics);
        //TypedBody must be computed *after* we know all the method signatures and such
        LateInit<TypedExpr, CompilationException> typedBody = new LateInit<>(() -> {
            checker.push();
            if (!isStatic) {
                if (!isConstructor() || !currentType.isPlural()) //Don't give a "this" local to plural-type constructors
                    checker.declare(loc, "this", currentType);
            }

            //If this is a constructor, the current type has an inheritance supertype, the supertype has a default constructor, and the first


            for (int i = 0; i < newParamTypes.size(); i++)
                checker.declare(loc, paramNames.get(i), newParamTypes.get(i));
            TypedExpr res = body.check(currentType, checker, generics, newReturnType);
            checker.pop();
            return res;
        });

        int disambiguationIndex = 0;
        for (TypeResolvedMethodDef method : allMethods) {
            if (this == method)
                break;
            if (method.name().equals(name))
                disambiguationIndex++;
        }
        return new SnuggleMethodDef(loc, pub, name, disambiguationIndex, numGenerics, isStatic, false, currentType, paramNames, newParamTypes, newReturnType, typedBody);
    }

}
