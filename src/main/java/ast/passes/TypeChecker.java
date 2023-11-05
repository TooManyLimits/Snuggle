package ast.passes;

import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.field.TypeResolvedFieldDef;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import ast.typed.def.type.GenericTypeDef;
import ast.typed.def.type.IndirectTypeDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.type_resolved.prog.TypeResolvedAST;
import ast.typed.prog.TypedAST;
import ast.typed.prog.TypedFile;
import builtin_types.BuiltinType;
import exceptions.compile_time.*;
import lexing.Loc;
import util.ListUtils;
import util.MapStack;
import util.MapUtils;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Responsible for converting a TypeResolvedAST into
 * the next stage: a TypedAST.
 *
 * Figures out the types of all expressions and such.
 * Tracks variables in scope and such things.
 */
public class TypeChecker {

    private final TypeResolvedAST ast;

    private TypeChecker(TypeResolvedAST ast) throws CompilationException {
        this.ast = ast;
    }

    private final MapStack<String, TypeDef> scopeVariables = new MapStack<>();
    public void push() { scopeVariables.push(); }
    public void pop() { scopeVariables.pop(); }
    public void declare(Loc loc, String name, TypeDef type) throws CompilationException {
        TypeDef prevType = scopeVariables.putIfAbsent(name, type);
        if (prevType != null)
            throw new AlreadyDeclaredException("Variable \"" + name + "\" is already declared in this scope!", loc);
    }

    //Returns null if the variable was not found
    public TypeDef lookup(String name) { //throws CompilationException {
//        TypeDef t = scopeVariables.get(name);
//        if (t == null)
//            throw new UndeclaredVariableException(errorMessage, loc);
//        return t;
        return scopeVariables.get(name);
    }

    //A cache for mapping ResolvedType -> TypeDef
    private final Map<Integer, Map<List<TypeDef>, TypeDef>> cache = new HashMap<>();
    //The set of all TypeDefs created here
    private final List<TypeDef> allTypeDefs = new ArrayList<>();

    public Collection<TypeDef> getAllInstantiated(ResolvedType.Basic basic) {
        Map<List<TypeDef>, TypeDef> cached = cache.get(basic.index());
        return cached == null ? List.of() : cached.values();
    }

    //Converts from ResolvedType to TypeDef.
    public TypeDef getOrInstantiate(ResolvedType resolvedType, List<TypeDef> typeGenerics) {
        if (resolvedType instanceof ResolvedType.Basic basic) {
            //Convert the generics and check if we've cached this already.
            //If we have, return that value.
            List<TypeDef> convertedGenerics = ListUtils.map(basic.generics(), g -> getOrInstantiate(g, typeGenerics));
            Map<List<TypeDef>, TypeDef> tMap = cache.get(basic.index());
            if (tMap != null) {
                TypeDef t = tMap.get(convertedGenerics);
                if (t != null)
                    return t;
            }
            //We haven't cached this yet, so let's compute and cache it.
            //Create the new type, but as an indirect. This adds a layer of indirection
            //to avoid problems in recursion.
            IndirectTypeDef resultType = new IndirectTypeDef();
            allTypeDefs.add(resultType);
            cache.computeIfAbsent(basic.index(), x -> new HashMap<>()).put(convertedGenerics, resultType);
            TypeDef instantiated = ast.typeDefs().get(basic.index()).instantiate(resultType, this, convertedGenerics);
            resultType.fill(instantiated);
            //And return
            return resultType;
        } else if (resolvedType instanceof ResolvedType.Generic generic) {
            if (generic.isMethod())
                return new GenericTypeDef(generic.index());
            else
                return typeGenerics.get(generic.index());
        } else {
            throw new IllegalStateException("Unexpected ResolvedType; bug in compiler, please report!");
        }
    }

    public TypeDef getBasicBuiltin(BuiltinType type) {
        return getOrInstantiate(new ResolvedType.Basic(ast.builtinIds().get(type), List.of()), List.of());
    }

    public TypeDef getReflectedBuiltin(Class<?> clazz) {
        return getBasicBuiltin(ast.reflectedBuiltins().get(clazz));
    }

    public TypeDef getGenericBuiltin(BuiltinType type, List<TypeDef> convertedGenerics) {
        //Similarly structured to above method getOrInstantiate()
        int index = ast.builtinIds().get(type);
        //Convert the generics and check if we've cached this already.
        //If we have, return that value.
        Map<List<TypeDef>, TypeDef> tMap = cache.get(index);
        if (tMap != null) {
            TypeDef t = tMap.get(convertedGenerics);
            if (t != null)
                return t;
        }
        //We haven't cached this yet, so let's compute and cache it.
        //Create the new type, but as an indirect. This adds a layer of indirection
        //to avoid problems in recursion.
        IndirectTypeDef resultType = new IndirectTypeDef();
        allTypeDefs.add(resultType);
        cache.computeIfAbsent(index, x -> new HashMap<>()).put(convertedGenerics, resultType);
        TypeDef instantiated = ast.typeDefs().get(index).instantiate(resultType, this, convertedGenerics);
        resultType.fill(instantiated);
        //And return
        return resultType;
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
        for (int i = 0; i < checker.allTypeDefs.size(); i++)
            checker.allTypeDefs.get(i).checkCode();
        //Return the result
        return new TypedAST(checker.allTypeDefs, typedFiles);
    }

    //Helper to check multiple method names, given as a list, in order.
    //If all of them fail, then error.
    //Throw a new error message, with the first error as the cause.
    public BestMethodInfo tryMultipleMethodsForBest(Loc loc, TypeDef currentType, TypeDef receiverType, List<String> methodNames, List<TypeResolvedExpr> args, List<ResolvedType> genericArgs, List<TypeDef> typeGenerics, boolean isCallStatic, boolean isSuperCall, TypeDef expectedReturnType) throws CompilationException {
        //Regular case, size = 1, just call the method normally and don't wrap in our weird error message
        if (methodNames.size() == 1)
            return getBestMethod(loc, currentType, receiverType, methodNames.get(0), args, genericArgs, typeGenerics, isCallStatic, isSuperCall, expectedReturnType);

        //Otherwise, do something else
        List<CompilationException> es = new ArrayList<>();
        for (String methodName : methodNames) {
            try {
                //If any succeeds, it gets returned
                return getBestMethod(loc, currentType, receiverType, methodName, args, genericArgs, typeGenerics, isCallStatic, isSuperCall, expectedReturnType);
            } catch (CompilationException e) {
                es.add(e);
            }
        }
        //If we finish the loop and none of them succeeded, then give our own error
        throw new TypeCheckingException("Unable to choose best method for any of: " + methodNames + ". Causes: " + ListUtils.map(es, Throwable::getMessage), loc);
    }

    /**
     * Helper method to deal with method overload resolution!
     * <p>
     * Tries to find the best method and return useful info about it.
     * If no best method can be chosen, instead throws a CompilationException.
     *
     * @param receiverType       The type of the receiver. This is what we look up methods on.
     * @param methodName         The name in the method call.
     * @param args               The TypeResolved arguments given to the method call. They will be checked here.
     * @param genericArgs        The generic arguments to the method call.
     * @param typeGenerics       The instantiating generic types that are passed to TypedExpr's check() and infer() methods.
     * @param isCallStatic       Whether the call was made statically.
     * @param isSuperCall        Whether this call to a method was made using the super keyword. If it was, then there's a
     *                           special process for selecting eligible methods.
     * @param expectedReturnType The type which the best method must return. If null, there's no restriction on the return type.
     */

    public BestMethodInfo getBestMethod(Loc loc, TypeDef currentType, TypeDef receiverType, String methodName, List<TypeResolvedExpr> args, List<ResolvedType> genericArgs, List<TypeDef> typeGenerics, boolean isCallStatic, boolean isSuperCall, TypeDef expectedReturnType) throws CompilationException {
        //First step: find a list of potentially matching methods we can call.

        List<MethodDef> matchingMethods = new ArrayList<>();
        //The cached typed args of the matching methods
        List<List<TypedExpr>> matchingMethodsTypedArgs = new ArrayList<>();
        //Create a cache for the check() lookups.
        //null map values mean that we already tried check()ing this, and it failed.
        List<IdentityHashMap<TypeDef, TypedExpr>> checkCache = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) checkCache.add(new IdentityHashMap<>());
        //Keep track of info for no-matching-method errors
        boolean foundCheckError = false;
        TypeCheckingException onlyCheckError = null;
        List<MethodDef> thrownOutDueToWrongReturnType = new ArrayList<>();

        //Get the list of methods to look through:
        List<? extends MethodDef> methodsToCheck;
        if (isSuperCall) {
            TypeDef def = receiverType;
            receiverType = def.inheritanceSupertype();
            if (receiverType == null)
                throw new IllegalStateException("Cannot use super, as this type has no supertype? Bug in compiler, please report!");
            def = receiverType;
            methodsToCheck = def.methods();
        } else {
            methodsToCheck = receiverType.getAllMethods();
        }

        //Loop over all these methods:
        outer:
        for (MethodDef def : methodsToCheck) {
            //Filter out ones which clearly don't match
            if (!def.name().equals(methodName)) continue; //Name doesn't match
            if (def.isStatic() != isCallStatic) continue; //static vs non-static
            if (def.paramTypes().size() != args.size()) continue; //Arity doesn't match
            if (!def.pub()) {
                if (def instanceof SnuggleMethodDef snuggleMethodDef) {
                    if (!snuggleMethodDef.loc().fileName().equals(loc.fileName()))
                        continue;
                } else
                    throw new IllegalStateException("Method defs aside from SnuggleMethodDef should always be pub! Bug in compiler, please report!");
            }
            //TODO: Add support for method generics. Taking things slow.
            if (def.numGenerics() > 0) throw new IllegalStateException("Generic methods not yet implemented");
            if (def.numGenerics() > 0 && genericArgs.size() != 0 && genericArgs.size() != def.numGenerics()) continue;

            //Now that we've filtered out those which obviously don't match, move on to less obvious situations.
            //First, invalid return types we can filter out:
            if (expectedReturnType != null) {
                //We have an expected type, so skip anything whose return type doesn't fit it
                if (!def.returnType().isSubtype(expectedReturnType)) {
                    thrownOutDueToWrongReturnType.add(def);
                    continue;
                }
            }

            //Now, let's check the arg types:
            List<TypedExpr> typedArgs = new ArrayList<>(args.size());
            int i = 0;
            TypeDef expectedParamType = null;
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
                            continue outer;
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
                if (!foundCheckError || onlyCheckError != null && e.getMessage().equals(onlyCheckError.getMessage())) {
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
            } else if (thrownOutDueToWrongReturnType.size() > 0) {
                //Need to look through the methods that were thrown out due to wrong return type, and use this
                //info to craft a nice error message for this situation.
                StringBuilder previouslyMatchingReturnTypes = new StringBuilder();
                for (MethodDef thrownOut : thrownOutDueToWrongReturnType) {
                    try {
                        //Iterate over the expected params and check() them all
                        for (int i = 0; i < thrownOut.paramTypes().size(); i++) {
                            TypeDef expectedParamType = thrownOut.paramTypes().get(i);
                            args.get(i).check(currentType, this, typeGenerics, expectedParamType);
                        }
                    } catch (TypeCheckingException e) {
                        continue;
                    }
                    previouslyMatchingReturnTypes.append(thrownOut.returnType().name());
                    previouslyMatchingReturnTypes.append(", ");
                }

                if (previouslyMatchingReturnTypes.length() > 0) {
                    String expectedTypeName = expectedReturnType.name();
                    previouslyMatchingReturnTypes.delete(previouslyMatchingReturnTypes.length() - 2, previouslyMatchingReturnTypes.length());
                    throw new TypeCheckingException("Expected method \"" + methodName + "\" to return " + expectedTypeName + ", but only found options " + previouslyMatchingReturnTypes, loc);
                }
            }
            throw new NoSuitableMethodException(methodName, isSuperCall, receiverType, loc);
        }

        //Exactly one method must have matched:
        return new BestMethodInfo(receiverType, matchingMethods.get(0), matchingMethodsTypedArgs.get(0));
    }

    //Holds the result of getBestMethod().
    //Contains all the information needed for the caller; particularly
    //the method def that was chosen as well as the type-checked arguments.
    public record BestMethodInfo(TypeDef receiverType, MethodDef methodDef, List<TypedExpr> typedArgs) {}

    /**
     * Attempt to choose the most specific method from the given list of methods.
     * If there is no most specific, throws a TooManyMethodsException.
     * The return value is the index of the most specific method.
     */
    private static int tryChoosingMostSpecific(Loc loc, String methodName, List<MethodDef> matchingMethods) throws CompilationException {
        //Create the indices array
        Integer[] arr = new Integer[matchingMethods.size()];
        for (int i = 0; i < arr.length; i++)
            arr[i] = i;

        //Sort, so the most specific wind up at the front
        Arrays.sort(arr, (a, b) -> matchingMethods.get(a).compareSpecificity(matchingMethods.get(b)));
        //If the first 2 are equally specific, error
        if (matchingMethods.get(arr[0]).compareSpecificity(matchingMethods.get(arr[1])) == 0)
            throw new TooManyMethodsException(methodName, loc);
        return arr[0];
    }


}
