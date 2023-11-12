package ast.type_resolved.expr;

import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedLiteral;
import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.def.method.MethodDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedMethodCall;
import lexing.Loc;
import util.ListUtils;
import util.throwing_interfaces.ThrowingSupplier;

import java.util.List;

public record TypeResolvedMethodCall(Loc loc, TypeResolvedExpr receiver, String methodName, ThrowingSupplier<TypeResolvedExpr, CompilationException> nextSupplier, List<ResolvedType> genericArgs, List<TypeResolvedExpr> args) implements TypeResolvedExpr {


    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        for (ResolvedType genericArg : genericArgs)
            verifier.verifyType(genericArg, loc);
        for (TypeResolvedExpr arg : args)
            arg.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Look up best method
        TypedExpr typedReceiver = receiver().infer(currentType, checker, typeGenerics, methodGenerics, cause);
        List<TypeDef> instantiatedGenericArgs = ListUtils.map(genericArgs, g -> checker.getOrInstantiate(g, typeGenerics, methodGenerics, loc, cause));
        TypeChecker.BestMethodInfo bestMethod;
        try {
            bestMethod = checker.getBestMethod(loc, currentType, typedReceiver.type(), methodName, args, instantiatedGenericArgs, typeGenerics, methodGenerics, false, false, null, cause);
        } catch (CompilationException e) {
            //If the method wasn't found, then try moving on to the next one using the nextSupplier
            if (nextSupplier == null)
                throw e;
            return nextSupplier.get().infer(currentType, checker, typeGenerics, methodGenerics, cause);
        }

        MethodDef matchingMethod = bestMethod.methodDef();
        List<TypedExpr> typedArgs = bestMethod.typedArgs();
        //Create the call
        TypedMethodCall call = new TypedMethodCall(loc, typedReceiver, matchingMethod, typedArgs, matchingMethod.returnType());
        //Const fold
        return matchingMethod.constantFold(call);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Look up best method
        TypedExpr typedReceiver = receiver().infer(currentType, checker, typeGenerics, methodGenerics, cause);
        List<TypeDef> instantiatedGenericArgs = ListUtils.map(genericArgs, g -> checker.getOrInstantiate(g, typeGenerics, methodGenerics, loc, cause));
        TypeChecker.BestMethodInfo bestMethod;
        try {
            bestMethod = checker.getBestMethod(loc, currentType, typedReceiver.type(), methodName, args, instantiatedGenericArgs, typeGenerics, methodGenerics, false, false, expected, cause);
        } catch (CompilationException e) {
            //If the method wasn't found, then try moving on to the next one using the nextSupplier
            if (nextSupplier == null)
                throw e;
            return nextSupplier.get().check(currentType, checker, typeGenerics, methodGenerics, expected, cause);
        }

        MethodDef matchingMethod = bestMethod.methodDef();
        List<TypedExpr> typedArgs = bestMethod.typedArgs();
        //Get the typed method call
        TypedMethodCall call = new TypedMethodCall(loc, typedReceiver, matchingMethod, typedArgs, matchingMethod.returnType());

        //Const fold
        TypedExpr res = matchingMethod.constantFold(call);
        if (res instanceof TypedLiteral typedLiteral) {
            //Pull type upwards if necessary; reasoning is explained in TypedLiteral
            return typedLiteral.pullTypeUpwards(expected);
        }
        return res;
    }


}