package ast.type_resolved.def.method;

import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.typed.Type;
import ast.typed.def.method.SnuggleMethodDef;
import ast.typed.expr.TypedExpr;
import lexing.Loc;
import util.LateInit;
import util.ListUtils;

import java.util.List;

public record SnuggleTypeResolvedMethodDef(Loc loc, boolean isStatic, String name, int numGenerics, List<String> paramNames, List<ResolvedType> paramTypes, ResolvedType returnType, TypeResolvedExpr body) implements TypeResolvedMethodDef {

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

    public SnuggleMethodDef instantiateType(Type currentType, TypeChecker checker, List<Type> generics) throws CompilationException {
        List<Type> newParamTypes = ListUtils.map(paramTypes, t -> checker.pool().getOrInstantiateType(t, generics));
        Type newReturnType = checker.pool().getOrInstantiateType(returnType, generics);
        //TypedBody must be computed *after* we know all the method signatures and such
        LateInit<TypedExpr, CompilationException> typedBody = new LateInit<>(() -> {
            checker.push();
            checker.declare(loc, "this", currentType);
            for (int i = 0; i < newParamTypes.size(); i++)
                checker.declare(loc, paramNames.get(i), newParamTypes.get(i));
            TypedExpr res = body.check(currentType, checker, generics, newReturnType);
            checker.pop();
            return res;
        });
        return new SnuggleMethodDef(loc, isStatic, name, numGenerics, paramNames, newParamTypes, newReturnType, typedBody);
    }

}
