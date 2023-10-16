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
        TypedExpr typedReceiver = receiver().infer(checker, typeGenerics);
        List<? extends MethodDef> matchingMethods = findMatching(typedReceiver.type(), methodName, args, genericArgs, checker, typeGenerics, false);

        MethodDef matchingMethod;
        //If there are multiple matching methods, error because we don't know which to call
        if (matchingMethods.size() > 1)
            matchingMethod = tryChoosingMostSpecific(loc, methodName, matchingMethods, checker);
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

        TypedMethodCall call = new TypedMethodCall(loc, typedReceiver, matchingMethod, typedArgs, matchingMethod.returnType());
        if (matchingMethod.isConst())
            return matchingMethod.doConst(call);
        return call;
    }

    @Override
    public TypedExpr check(TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        TypedExpr typedReceiver = receiver().infer(checker, typeGenerics);
        List<? extends MethodDef> matchingMethods = findMatching(typedReceiver.type(), methodName, args, genericArgs, checker, typeGenerics, false);

        //Get info for error reporting
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

        //If there are multiple matching methods, error because we don't know which to call
        if (matchingMethods.size() > 1) {
            matchingMethod = tryChoosingMostSpecific(loc, methodName, matchingMethods, checker);
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

        TypedMethodCall call = new TypedMethodCall(loc, typedReceiver, matchingMethod, typedArgs, matchingMethod.returnType());
        //Do const folding if it's a const method
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

    //Find the list of potentially matching methods
    public static List<? extends MethodDef> findMatching(Type receiverType, String methodName, List<TypeResolvedExpr> args, List<ResolvedType> genericArgs, TypeChecker checker, List<Type> typeGenerics, boolean isStaticCall) throws CompilationException {
        //Look up methods on it, filter out any non-matches
        List<? extends MethodDef> matchingMethods = ListUtils.filter(checker.pool().getTypeDef(receiverType).getAllMethods(checker.pool()), method -> {
            //Proceed with tests, in order of most obviously wrong -> least obviously wrong, to avoid as many calculations as possible
            //Name doesn't match
            if (!method.name().equals(methodName))
                return false;
            //Arity doesn't match
            if (method.paramTypes().size() != args.size())
                return false;
            //Static vs non-static call
            if (method.isStatic() != isStaticCall)
                return false;

            //TODO: Remove. Temporary thing since we don't have generic methods implemented yet.
            //TODO: One step at a time, everyone. Have patience.
            if (method.numGenerics() > 0)
                throw new IllegalStateException("Generic methods not yet implemented");

            //Method is generic, we supply generic args, and we supply the wrong number
            if (method.numGenerics() > 0 && genericArgs.size() != 0 && genericArgs.size() != method.numGenerics())
                return false;

            //Otherwise, it's time to try the args and see if they work...
            //TODO: Cache results of annotatedType checking to avoid unneeded checking of the same param expr with the same expected annotatedType
            //TODO: Also cache the outputs of the check() functions of successful attempts so we don't have to call it again
            try {
                List<Type> expectedParams = method.paramTypes();
                for (int i = 0; i < expectedParams.size(); i++) {
                    Type expected = expectedParams.get(i);
                    //Check
                    args.get(i).check(checker, typeGenerics, expected);
                }
                //All the checking succeeded with no error, so move on
            } catch (TypeCheckingException e) {
                //If check() failed, then this method doesn't work. Return false
                return false;
            }
            return true;
        });

        //Return all needed info
        return matchingMethods;
    }

    //If only one check() was performed inside the try-catch, and it failed,
    //then save the only check's error in the field.
    //We can report it later.
    //TODO IN THE MORNING: Make use of this to improve error messages!
    public record MatchingMethodsInfo(List<? extends MethodDef> matchingMethods, TypeCheckingException onlyCheckError) {}

    // Attempt to choose the most specific method from the list of matching methods
    public static MethodDef tryChoosingMostSpecific(Loc loc, String methodName, List<? extends MethodDef> matchingMethods, TypeChecker checker) throws CompilationException {
        //Sort the methods, so the most specific wind up at the front
        matchingMethods.sort((a, b) -> a.compareSpecificity(b, checker));
        //If the first 2 are equally specific, error
        if (matchingMethods.get(0).compareSpecificity(matchingMethods.get(1), checker) == 0)
            throw new TooManyMethodsException("Unable to determine which overload of \"" + methodName + "\" to call based on provided args and context", loc);
        return matchingMethods.get(0);
    }


}