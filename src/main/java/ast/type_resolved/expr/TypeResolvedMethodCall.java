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

import java.util.List;

public record TypeResolvedMethodCall(Loc loc, TypeResolvedExpr receiver, String methodName, List<ResolvedType> genericArgs, List<TypeResolvedExpr> args) implements TypeResolvedExpr {
    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        for (ResolvedType genericArg : genericArgs)
            verifier.verifyType(genericArg, loc);
        for (TypeResolvedExpr arg : args)
            arg.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics) throws CompilationException {
        //Look up best method
        TypedExpr typedReceiver = receiver().infer(currentType, checker, typeGenerics);
        TypeChecker.BestMethodInfo bestMethod = checker.getBestMethod(loc, currentType, typedReceiver.type(), methodName, args, genericArgs, typeGenerics, false, false, null);
        MethodDef matchingMethod = bestMethod.methodDef();
        List<TypedExpr> typedArgs = bestMethod.typedArgs();
        //Create the call
        TypedMethodCall call = new TypedMethodCall(loc, typedReceiver, matchingMethod, typedArgs, matchingMethod.returnType());
        //Const fold
        return matchingMethod.constantFold(call);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected) throws CompilationException {
        //Look up best method
        TypedExpr typedReceiver = receiver().infer(currentType, checker, typeGenerics);
        TypeChecker.BestMethodInfo bestMethod = checker.getBestMethod(loc, currentType, typedReceiver.type(), methodName, args, genericArgs, typeGenerics, false, false, expected);
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