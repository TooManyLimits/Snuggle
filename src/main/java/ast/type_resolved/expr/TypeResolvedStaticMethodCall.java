package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedStaticMethodCall;
import exceptions.CompilationException;
import exceptions.NoSuitableMethodException;
import exceptions.TypeCheckingException;
import lexing.Loc;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;

//The type field is never generic; can't call static methods on generic type because there will be no parameters
public record TypeResolvedStaticMethodCall(Loc loc, ResolvedType type, String methodName, List<ResolvedType> genericArgs, List<TypeResolvedExpr> args) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        for (ResolvedType genericArg : genericArgs)
            verifier.verifyType(genericArg, loc);
        for (TypeResolvedExpr arg : args)
            arg.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(Type currentType, TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        //Lookup best method
        Type receiverType = checker.pool().getOrInstantiateType(type, typeGenerics);
        TypeChecker.BestMethodInfo bestMethod = checker.getBestMethod(loc, currentType, receiverType, methodName, args, genericArgs, typeGenerics, true, null, 0);
        MethodDef matchingMethod = bestMethod.methodDef();
        List<TypedExpr> typedArgs = bestMethod.typedArgs();
        //Create typed call
        TypedStaticMethodCall call = new TypedStaticMethodCall(loc, receiverType, matchingMethod, typedArgs, matchingMethod.returnType());
        //Const fold if possible
        if (matchingMethod.isConst())
            return matchingMethod.doConstStatic(call);
        return call;
    }

    @Override
    public TypedExpr check(Type currentType, TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        //Lookup best method
        Type receiverType = checker.pool().getOrInstantiateType(type, typeGenerics);
        TypeChecker.BestMethodInfo bestMethod = checker.getBestMethod(loc, currentType, receiverType, methodName, args, genericArgs, typeGenerics, true, expected, 0);
        MethodDef matchingMethod = bestMethod.methodDef();
        List<TypedExpr> typedArgs = bestMethod.typedArgs();
        //Create typed call
        TypedStaticMethodCall call = new TypedStaticMethodCall(loc, receiverType, matchingMethod, typedArgs, matchingMethod.returnType());
        //Const fold if possible
        if (matchingMethod.isConst()) {
            TypedExpr res = matchingMethod.doConstStatic(call);
            //Pull type upwards if necessary; reasoning is explained in TypedLiteral
            if (res instanceof TypedLiteral typedLiteral) {
                return typedLiteral.pullTypeUpwards(expected);
            }
            return res;
        }
        return call;

    }
}
