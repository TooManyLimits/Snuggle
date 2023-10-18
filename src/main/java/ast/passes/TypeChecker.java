package ast.passes;

import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import exceptions.*;
import ast.type_resolved.prog.TypeResolvedAST;
import ast.typed.Type;
import ast.typed.prog.TypedAST;
import ast.typed.prog.TypedFile;
import lexing.Loc;
import util.ListUtils;
import util.MapStack;
import util.MapUtils;

import java.util.*;

/**
 * Responsible for converting a TypeResolvedAST into
 * the next stage: a TypedAST.
 *
 * Figures out the types of all expressions and such.
 */
public class TypeChecker {

    // The annotatedType pool, keeps track of all the types in existence
    private final TypePool typePool;

    private TypeChecker(TypeResolvedAST ast) throws CompilationException {
        typePool = new TypePool(this, ast);
    }

    public TypePool pool() {
        return typePool;
    }

    private final MapStack<String, Type> scopeVariables = new MapStack<>();
    public void push() { scopeVariables.push(); }
    public void pop() { scopeVariables.pop(); }
    public void declare(Loc loc, String name, Type type) throws CompilationException {
        Type prevType = scopeVariables.putIfAbsent(name, type);
        if (prevType != null)
            throw new AlreadyDeclaredException("Variable \"" + name + "\" is already declared in this scope!", loc);
    }

    public Type lookup(Loc loc, String name) throws CompilationException {
        Type t = scopeVariables.get(name);
        if (t == null)
            throw new UndeclaredVariableException("Variable \"" + name + "\" was not declared in this scope", loc);
        return t;
    }

    /**
     * Method to fully convert a TypeResolvedAST into a TypedAST.
     */
    public static TypedAST type(TypeResolvedAST resolvedAST) throws CompilationException {
        //Create the checker
        TypeChecker checker = new TypeChecker(resolvedAST);
        //Type check all the top-level code
        Map<String, TypedFile> typedFiles = MapUtils.mapValues(resolvedAST.files(), file -> file.type(checker));
        //Type check the method bodies, repeatedly, until there are no more
        //NOTE: This cannot be an enhanced for loop! Because:
        //while we check method bodies, *more checked type defs can be added to the list*.
        //If we used an enhanced loop, this would lead to concurrent modification exceptions.
        //However, this way, since new types are always appended to the end, we continue
        //checking method bodies until no new method bodies are added to check, and we reach
        //the end of the list.
        List<TypeDef> checkedTypeDefs = checker.pool().getFinalTypeDefs();
        for (int i = 0; i < checkedTypeDefs.size(); i++)
            checkedTypeDefs.get(i).checkMethodBodies();
        //Return the result
        return new TypedAST(checkedTypeDefs, typedFiles);
    }

    /**
     *
     * Helper method to deal with method overload resolution!
     *
     * Tries to find the best method and return useful info about it.
     * If no best method can be chosen, instead throws a CompilationException.
     *
     * @param receiverType The type of the receiver. This is what we look up methods on.
     * @param methodName The name in the method call.
     * @param args The TypeResolved arguments given to the method call. They will be checked here.
     * @param genericArgs The generic arguments to the method call.
     * @param typeGenerics The instantiating generic types that are passed to TypedExpr's check() and infer() methods.
     * @param isCallStatic Whether the call was made statically.
     * @param expectedReturnType The type which the best method must return. If null, there's no restriction on the return type.
     * @param superLookupDepth The number of superclass levels up to look. If 0, then looks at all methods, including inherited ones.
     *                         If this number is greater than the number of actual supertypes, throws an error.
     */

    public BestMethodInfo getBestMethod(Loc loc, Type currentType, Type receiverType, String methodName, List<TypeResolvedExpr> args, List<ResolvedType> genericArgs, List<Type> typeGenerics, boolean isCallStatic, Type expectedReturnType, int superLookupDepth) throws CompilationException {
        //First step: find a list of potentially matching methods we can call.

        List<MethodDef> matchingMethods = new ArrayList<>();
        //The cached typed args of the matching methods
        List<List<TypedExpr>> matchingMethodsTypedArgs = new ArrayList<>();
        //Create a cache for the check() lookups.
        //null map values mean that we already tried check()ing this, and it failed.
        List<HashMap<Type, TypedExpr>> checkCache = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) checkCache.add(new HashMap<>());
        //Keep track of info for no-matching-method errors
        boolean foundCheckError = false;
        TypeCheckingException onlyCheckError = null;
        List<MethodDef> thrownOutDueToWrongReturnType = null;

        //Get the list of methods to look through:
        boolean isSuperLookup = superLookupDepth > 0;
        List<? extends MethodDef> methodsToCheck;
        if (isSuperLookup) {
            int remainingLookupDepth = superLookupDepth;
            TypeDef cur = pool().getTypeDef(receiverType);
            while (remainingLookupDepth > 0) {
                receiverType = cur.trueSupertype();
                if (receiverType == null)
                    throw new NoSuitableMethodException("Cannot look up " + superLookupDepth + " levels into supertypes; only " + (superLookupDepth - remainingLookupDepth) + " exist.", loc);
                cur = pool().getTypeDef(receiverType);
                remainingLookupDepth--;
            }
            methodsToCheck = cur.getMethods();
        } else {
            methodsToCheck = pool().getTypeDef(receiverType).getAllMethods(pool());
        }

        //Loop over all these methods:
        for (MethodDef def : methodsToCheck) {
            //Filter out ones which clearly don't match
            if (!def.name().equals(methodName)) continue; //Name doesn't match
            if (def.isStatic() != isCallStatic) continue; //static vs non-static
            if (def.paramTypes().size() != args.size()) continue; //Arity doesn't match
            //TODO: Add support for method generics. Taking things slow.
            if (def.numGenerics() > 0) throw new IllegalStateException("Generic methods not yet implemented");
            if (def.numGenerics() > 0 && genericArgs.size() != 0 && genericArgs.size() != def.numGenerics()) continue;

            //Now that we've filtered out those which obviously don't match, move on to less obvious situations.
            //First, invalid return types we can filter out:
            if (expectedReturnType != null) {
                //We have an expected type, so skip anything whose return type doesn't fit it
                if (!def.returnType().isSubtype(expectedReturnType, pool())) {
                    if (thrownOutDueToWrongReturnType == null)
                        thrownOutDueToWrongReturnType = new ArrayList<>();
                    thrownOutDueToWrongReturnType.add(def);
                    continue;
                }
            }

            //Now, let's check the arg types:
            List<TypedExpr> typedArgs = new ArrayList<>(args.size());
            int i = 0;
            Type expectedParamType = null;
            try {
                //Iterate over the expected params...
                for (i = 0; i < def.paramTypes().size(); i++) {
                    //Get the expected param type
                    expectedParamType = def.paramTypes().get(i);
                    //check() the arg against the param.
                    //If we've already check()ed the same arg with the same param before,
                    //then we can reuse that.
                    TypedExpr typedArg = checkCache.get(i).get(expectedParamType);
                    if (typedArg == null) {
                        if (checkCache.get(i).containsKey(expectedParamType)) {
                            //We already tried calling check() on this, and it failed. Continue.
                            continue;
                        }
                        //We haven't checked this arg with this type yet, so do it now:
                        typedArg = args.get(i).check(currentType, this, typeGenerics, expectedParamType);
                        //If that didn't throw an exception, then add it to the cache
                        checkCache.get(i).put(expectedParamType, typedArg);
                    }
                    //Add it to the list of typed args
                    typedArgs.add(typedArg);
                }
            } catch (TypeCheckingException e) {
                //A type check failed, so this method def is not applicable.
                //Note this information down and continue.
                checkCache.get(i).put(expectedParamType, null);
                if (!foundCheckError) {
                    foundCheckError = true;
                    onlyCheckError = e;
                } else {
                    onlyCheckError = null;
                }
                continue;
            }

            //All the checks succeeded, so let's add this to our lists:
            matchingMethods.add(def);
            matchingMethodsTypedArgs.add(typedArgs);
        }

        //Now that that's done, let's take a look at our methods which matched.

        //Multiple methods matched:
        if (matchingMethods.size() > 1) {
            //If there are multiple matching methods, try choosing the most specific one.
            //If there is no most specific one, the following errors.
            int bestIndex = tryChoosingMostSpecific(loc, methodName, matchingMethods);
            return new BestMethodInfo(receiverType, matchingMethods.get(bestIndex), matchingMethodsTypedArgs.get(bestIndex));
        }

        //No methods matched:
        if (matchingMethods.size() == 0) {
            //If there are no matching methods, error since we couldn't find any applicable one.
            if (foundCheckError && onlyCheckError != null) {
                //In this case, throw the only check error we ran into:
                throw onlyCheckError;
            } else if (thrownOutDueToWrongReturnType != null) {
                //Need to look through the methods that were thrown out due to wrong return type, and use this
                //info to craft a nice error message for this situation.
                StringBuilder previouslyMatchingReturnTypes = new StringBuilder();
                for (MethodDef thrownOut : thrownOutDueToWrongReturnType) {
                    try {
                        //Iterate over the expected params and check() them all
                        for (int i = 0; i < thrownOut.paramTypes().size(); i++) {
                            Type expectedParamType = thrownOut.paramTypes().get(i);
                            args.get(i).check(currentType, this, typeGenerics, expectedParamType);
                        }
                    } catch (TypeCheckingException e) {
                        continue;
                    }
                    previouslyMatchingReturnTypes.append(thrownOut.returnType().name(pool()));
                    previouslyMatchingReturnTypes.append(", ");
                }

                if (previouslyMatchingReturnTypes.length() > 0) {
                    String expectedTypeName = expectedReturnType.name(pool());
                    previouslyMatchingReturnTypes.delete(previouslyMatchingReturnTypes.length() - 2, previouslyMatchingReturnTypes.length());
                    throw new TypeCheckingException("Expected method \"" + methodName + "\" to return " + expectedTypeName + ", but only found options " + previouslyMatchingReturnTypes, loc);
                }
            }
            if (methodName.equals("new")) {
                if (isSuperLookup)
                    throw new NoSuitableMethodException("Unable to find suitable super() constructor for provided args", loc);
                else
                    throw new NoSuitableMethodException("Unable to find suitable constructor for provided args", loc);
            } else
                throw new NoSuitableMethodException("Unable to find suitable method \"" + methodName + "\" for provided args", loc);
        }

        //Exactly one method must have matched:
        return new BestMethodInfo(receiverType, matchingMethods.get(0), matchingMethodsTypedArgs.get(0));
    }

    //Holds the result of getBestMethod().
    //Contains all the information needed for the caller; particularly
    //the method def that was chosen as well as the type-checked arguments.
    public record BestMethodInfo(Type receiverType, MethodDef methodDef, List<TypedExpr> typedArgs) {}

    /**
     * Attempt to choose the most specific method from the given list of methods.
     * If there is no most specific, throws a TooManyMethodsException.
     * The return value is the index of the most specific method.
     */
    private int tryChoosingMostSpecific(Loc loc, String methodName, List<MethodDef> matchingMethods) throws CompilationException {
        //Create the indices array
        Integer[] arr = new Integer[matchingMethods.size()];
        for (int i = 0; i < arr.length; i++)
            arr[i] = i;

        //Sort, so the most specific wind up at the front
        Arrays.sort(arr, (a, b) -> matchingMethods.get(a).compareSpecificity(matchingMethods.get(b), this));
        //If the first 2 are equally specific, error
        if (matchingMethods.get(arr[0]).compareSpecificity(matchingMethods.get(arr[1]), this) == 0)
            throw new TooManyMethodsException("Unable to determine which overload of \"" + methodName + "\" to call based on provided args and context", loc);
        return arr[0];
    }


}
