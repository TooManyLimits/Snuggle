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
    public TypedExpr infer(TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        Type receiverType = checker.pool().getOrInstantiateType(type, typeGenerics);
        List<? extends MethodDef> matchingMethods = TypeResolvedMethodCall.findMatching(receiverType, methodName, args, genericArgs, checker, typeGenerics, true);

        MethodDef matchingMethod;
        //If there are multiple matching methods, error because we don't know which to call
        if (matchingMethods.size() > 1)
            matchingMethod = TypeResolvedMethodCall.tryChoosingMostSpecific(loc, methodName, matchingMethods, checker);
            //If there are no matching methods, error since we couldn't find any applicable one
        else if (matchingMethods.size() == 0)
            throw new NoSuitableMethodException("Unable to find suitable method \"" + methodName + "\" for provided args", loc);
        else
            matchingMethod = matchingMethods.get(0);

        //There's only one matching method; return the proper TypedExpr method call
        //TODO: Store this list of typedArgs in MatchingInfo and fetch; it was already computed

        List<Type> expectedParams = matchingMethod.paramTypes();
        List<TypedExpr> typedArgs = new ArrayList<>(args.size());
        for (int i = 0; i < expectedParams.size(); i++) {
            typedArgs.add(args.get(i).check(checker, typeGenerics, expectedParams.get(i)));
        }

        TypedStaticMethodCall call = new TypedStaticMethodCall(loc, receiverType, matchingMethod, typedArgs, matchingMethod.returnType());
        if (matchingMethod.isConst())
            return matchingMethod.doConstStatic(call);
        return call;
    }

    @Override
    public TypedExpr check(TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        Type receiverType = checker.pool().getOrInstantiateType(type, typeGenerics);
        List<? extends MethodDef> matchingMethods = TypeResolvedMethodCall.findMatching(receiverType, methodName, args, genericArgs, checker, typeGenerics, true);

        int numPreviouslyMatching = matchingMethods.size();
        String previouslyMatchingReturnTypes = null;
        if (numPreviouslyMatching == 1)
            previouslyMatchingReturnTypes = matchingMethods.get(0).returnType().name(checker.pool());
        else if (numPreviouslyMatching > 1) {
            StringBuilder b = new StringBuilder(matchingMethods.get(0).returnType().name(checker.pool()));
            for (int i = 1; i < matchingMethods.size(); i++)
                b.append(", ").append(matchingMethods.get(i).returnType().name(checker.pool()));
            previouslyMatchingReturnTypes = b.toString();
        }

        //Filter the list further to find only methods where the return annotatedType matches the expected output:
        matchingMethods = ListUtils.filter(matchingMethods,
                method -> method.returnType().isSubtype(expected, checker.pool())
        );

        MethodDef matchingMethod;
        //If there are multiple matching methods, try choosing the most specific
        if (matchingMethods.size() > 1) {
            matchingMethod = TypeResolvedMethodCall.tryChoosingMostSpecific(loc, methodName, matchingMethods, checker);
        } else if (matchingMethods.size() == 0) {
            //If there are no matching methods, error since we couldn't find any applicable one
            if (numPreviouslyMatching > 0) {
                //Problem was with the return annotatedType not matching, so let's make a special error message
                throw new TypeCheckingException("Expected method \"" + methodName + "\" to return " + expected.name(checker.pool()) + ", but only found options " + previouslyMatchingReturnTypes, loc);
            } else {
                throw new NoSuitableMethodException("Unable to find suitable method \"" + methodName + "\" for provided args", loc);
            }
        } else {
            matchingMethod = matchingMethods.get(0);
        }

        //There's only one matching method; return the proper TypedExpr method call
        //TODO: Store this list of typedArgs in MatchingInfo and fetch; it was already computed

        List<Type> expectedParams = matchingMethod.paramTypes();
        List<TypedExpr> typedArgs = new ArrayList<>(args.size());
        for (int i = 0; i < expectedParams.size(); i++) {
            typedArgs.add(args.get(i).check(checker, typeGenerics, expectedParams.get(i)));
        }

        TypedStaticMethodCall call = new TypedStaticMethodCall(loc, receiverType, matchingMethod, typedArgs, matchingMethod.returnType());

        //Do const folding if it's a const method
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
