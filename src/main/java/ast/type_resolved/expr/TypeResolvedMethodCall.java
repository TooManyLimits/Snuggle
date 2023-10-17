package ast.type_resolved.expr;

import ast.typed.expr.TypedLiteral;
import exceptions.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedMethodCall;
import exceptions.NoSuitableMethodException;
import exceptions.TooManyMethodsException;
import exceptions.TypeCheckingException;
import lexing.Loc;
import util.ListUtils;

import java.util.ArrayList;
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
    public TypedExpr infer(TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        //Look up best method
        TypedExpr typedReceiver = receiver().infer(checker, typeGenerics);
        TypeChecker.BestMethodInfo bestMethod = checker.getBestMethod(loc, typedReceiver.type(), methodName, args, genericArgs, typeGenerics, false, null);
        MethodDef matchingMethod = bestMethod.methodDef();
        List<TypedExpr> typedArgs = bestMethod.typedArgs();
        //Create the call
        TypedMethodCall call = new TypedMethodCall(loc, typedReceiver, matchingMethod, typedArgs, matchingMethod.returnType());
        //Const fold
        if (matchingMethod.isConst())
            return matchingMethod.doConst(call);
        return call;
    }

    @Override
    public TypedExpr check(TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        //Look up best method
        TypedExpr typedReceiver = receiver().infer(checker, typeGenerics);
        TypeChecker.BestMethodInfo bestMethod = checker.getBestMethod(loc, typedReceiver.type(), methodName, args, genericArgs, typeGenerics, false, expected);
        MethodDef matchingMethod = bestMethod.methodDef();
        List<TypedExpr> typedArgs = bestMethod.typedArgs();
        //Get the typed method call
        TypedMethodCall call = new TypedMethodCall(loc, typedReceiver, matchingMethod, typedArgs, matchingMethod.returnType());
        //Const fold
        if (matchingMethod.isConst()) {
            TypedExpr res = matchingMethod.doConst(call);
            //Pull type upwards if necessary; reasoning is explained in TypedLiteral
            if (res instanceof TypedLiteral typedLiteral) {
                return typedLiteral.pullTypeUpwards(expected);
            }
            return res;
        }
        return call;
    }


}