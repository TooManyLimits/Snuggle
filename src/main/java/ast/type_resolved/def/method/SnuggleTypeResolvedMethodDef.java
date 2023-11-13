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
import util.LateInitFunction;
import util.ListUtils;

import java.util.List;

public record SnuggleTypeResolvedMethodDef(Loc loc, boolean pub, boolean isStatic, String name, int numGenerics, List<String> paramNames, List<ResolvedType> paramTypes, ResolvedType returnType, TypeResolvedExpr body) implements TypeResolvedMethodDef {

    //Same as in other topLevelTypes. Error if there's a violation.
    public void verifyGenericCounts(GenericVerifier verifier) throws CompilationException {
        //Verify return annotatedType
        verifier.verifyType(returnType, loc);
        //Verify param topLevelTypes
        for (ResolvedType t : paramTypes)
            verifier.verifyType(t, loc);
        //Verify body
        body.verifyGenericArgCounts(verifier);
    }

    //allMethods are passed in for disambiguating between method names.
    public SnuggleMethodDef instantiateType(List<? extends TypeResolvedMethodDef> allMethods, TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef.InstantiationStackFrame cause) {
        LateInitFunction<List<TypeDef>, List<TypeDef>, RuntimeException> newParamTypes = new LateInitFunction<>(methodGenerics -> ListUtils.map(paramTypes, t -> checker.getOrInstantiate(t, typeGenerics, methodGenerics, loc, cause)));
        LateInitFunction<List<TypeDef>, TypeDef, RuntimeException> newReturnType = new LateInitFunction<>(methodGenerics -> checker.getOrInstantiate(returnType, typeGenerics, methodGenerics, loc, cause));
        //TypedBody must be computed later, once we know method signatures and such
        LateInitFunction<List<TypeDef>, TypedExpr, CompilationException> typedBody = new LateInitFunction<>(methodGenerics -> {
            checker.pushNewEnv(false);
            if (!isStatic) {
                if (!isConstructor() || !currentType.isPlural()) //Don't give a "this" local to plural-type constructors
                    checker.declare(loc, "this", currentType);
            }

            for (int i = 0; i < paramNames.size(); i++)
                checker.declare(loc, paramNames.get(i), newParamTypes.get(methodGenerics).get(i));
            TypedExpr res = body.check(currentType, checker, typeGenerics, methodGenerics, newReturnType.get(methodGenerics), cause);
            checker.popEnv();
            return res;
        });

        int disambiguationIndex = 0;
        for (TypeResolvedMethodDef method : allMethods) {
            if (this == method)
                break;
            if (method.name().equals(name))
                disambiguationIndex++;
        }
        return new SnuggleMethodDef(loc, pub, name, disambiguationIndex, numGenerics, isStatic, false, currentType, paramNames.size(), paramNames, newParamTypes, newReturnType, typedBody);
    }

}
