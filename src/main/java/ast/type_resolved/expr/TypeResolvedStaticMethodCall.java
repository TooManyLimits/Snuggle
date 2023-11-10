package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedStaticMethodCall;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

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
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Lookup best method
        TypeDef receiverType = checker.getOrInstantiate(type, typeGenerics, loc, cause);
        TypeChecker.BestMethodInfo bestMethod = checker.getBestMethod(loc, currentType, receiverType, methodName, args, genericArgs, typeGenerics, true, false, null, cause);
        MethodDef matchingMethod = bestMethod.methodDef();
        List<TypedExpr> typedArgs = bestMethod.typedArgs();
        //Create typed call
        TypedStaticMethodCall call = new TypedStaticMethodCall(loc, receiverType, matchingMethod, typedArgs, matchingMethod.returnType());
        //Const fold
        return matchingMethod.constantFold(call);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Lookup best method
        TypeDef receiverType = checker.getOrInstantiate(type, typeGenerics, loc, cause);
        TypeChecker.BestMethodInfo bestMethod = checker.getBestMethod(loc, currentType, receiverType, methodName, args, genericArgs, typeGenerics, true, false, expected, cause);
        MethodDef matchingMethod = bestMethod.methodDef();
        List<TypedExpr> typedArgs = bestMethod.typedArgs();
        //Create typed call
        TypedStaticMethodCall call = new TypedStaticMethodCall(loc, receiverType, matchingMethod, typedArgs, matchingMethod.returnType());
        //Const fold if possible
        TypedExpr res = matchingMethod.constantFold(call);
        if (res instanceof TypedLiteral typedLiteral) {
            //Pull type upwards if necessary; reasoning is explained in TypedLiteral
            return typedLiteral.pullTypeUpwards(expected);
        }
        return res;

    }
}
