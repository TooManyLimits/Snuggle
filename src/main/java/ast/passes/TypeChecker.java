package ast.passes;

import ast.passes.typeinference.TypeInferenceContext;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedExtensionMethod;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import ast.typed.def.type.FuncTypeDef;
import ast.typed.def.type.IndirectTypeDef;
import ast.typed.def.type.TupleTypeDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.type_resolved.prog.TypeResolvedAST;
import ast.typed.prog.TypedAST;
import ast.typed.prog.TypedFile;
import builtin_types.BuiltinType;
import builtin_types.BuiltinTypes;
import builtin_types.types.ExtensionMethods;
import exceptions.compile_time.*;
import lexing.Loc;
import util.*;

import java.util.*;

/**
 * Responsible for converting a TypeResolvedAST into
 * the next stage: a TypedAST.
 *
 * Figures out the topLevelTypes of all expressions and such.
 * Tracks variables in scope and such things.
 */
public class TypeChecker {

    private final TypeResolvedAST ast;

    private TypeChecker(TypeResolvedAST ast) throws CompilationException {
        this.ast = ast;
    }

    private final Stack<MapStack<String, TypeDef>> scopeVariables = new Stack<>();
    private final Stack<Boolean> isLambdaEnv = new Stack<>();
    private final Stack<Stack<List<MethodDef>>> extensionMethods = new Stack<>();
    private final Stack<LateInit<TypeDef, CompilationException>> desiredReturnTypes = new Stack<>();
    private final Stack<Set<TypeDef>> attemptedReturnTypes = new Stack<>();

    public void push() {
        scopeVariables.peek().push();
        extensionMethods.peek().push(new ArrayList<>());
    }
    public void pushNewEnv(boolean isLambda, LateInit<TypeDef, CompilationException> desiredReturnType) {
        scopeVariables.push(new MapStack<>());
        isLambdaEnv.push(isLambda);
        extensionMethods.push(new Stack<>());
        extensionMethods.peek().add(new ArrayList<>());
        desiredReturnTypes.push(desiredReturnType);
        attemptedReturnTypes.push(isLambda ? new HashSet<>() : null);
    }
    public void pop() {
        scopeVariables.peek().pop();
        extensionMethods.peek().pop();
    }
    public void popEnv() {
        scopeVariables.pop();
        isLambdaEnv.pop();
        extensionMethods.pop();
        desiredReturnTypes.pop();
        attemptedReturnTypes.pop();
    }
    public MapStack<String, TypeDef> peekEnv() {
        return scopeVariables.peek();
    }
    public void declare(Loc loc, String name, TypeDef type) throws CompilationException {
        TypeDef prevType = scopeVariables.peek().putIfAbsent(name, type);
        if (prevType != null)
            throw new AlreadyDeclaredException("Variable \"" + name + "\" is already declared in this scope!", loc);
    }
    //Returns null if the variable was not found
    public TypeDef lookup(String name) { //throws CompilationException {
        return scopeVariables.peek().get(name);
    }
    public boolean isLambda() {
        return isLambdaEnv.peek();
    }
    public void addExtensionMethod(SnuggleMethodDef extensionMethod) {
        extensionMethods.peek().peek().add(extensionMethod);
    }
    public void importExtensionMethods(String fileName, boolean importNonPub, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        for (TypeResolvedExtensionMethod extensionMethod : ast.files().get(fileName).topLevelExtensionMethods()) {
            //If the method is pub, or we're importing non-pub things, then add it:
            if (extensionMethod.typeResolvedMethodDef().pub() || importNonPub) {
                //Call infer. The implementation of infer shouldn't need any information
                //other than the checker and the cause, which are known here.
                extensionMethod.addToCheckerEnvironment(this, List.of(), cause, true);
            }
        }
    }
    public LateInit<TypeDef, CompilationException> getDesiredReturnType() {
        return desiredReturnTypes.peek();
    }
    public Set<TypeDef> getAttemptedReturnTypes() {
        return attemptedReturnTypes.peek();
    }

    //A cache for mapping (ResolvedType indices, generics) -> TypeDef
    private final Map<Integer, LinkedHashMap<List<TypeDef>, TypeDef>> cache = new HashMap<>();
    private final Map<List<TypeDef>, TypeDef> tupleCache = new HashMap<>();
    private final Map<List<TypeDef>, Map<TypeDef, TypeDef>> funcCache = new HashMap<>();
    //The set of all TypeDefs created here
    private final List<TypeDef> allTypeDefs = new ArrayList<>();

    public Collection<TypeDef> getAllInstantiated(ResolvedType.Basic basic) {
        Map<List<TypeDef>, TypeDef> cached = cache.get(basic.index());
        return cached == null ? List.of() : cached.values();
    }

    //Converts from ResolvedType to TypeDef.
    public TypeDef getOrInstantiate(ResolvedType resolvedType, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        if (resolvedType instanceof ResolvedType.Basic basic) {
            //Convert the generics and check if we've cached this already.
            //If we have, return that value.
            List<TypeDef> convertedGenerics = ListUtils.map(basic.generics(), g -> getOrInstantiate(g, typeGenerics, methodGenerics, instantiationLoc, cause));
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

            //Only save this type if NONE of the elements are unknown generics.
            if (!ListUtils.any(convertedGenerics, TypeInferenceContext::containsUnknownGeneric)) {
                allTypeDefs.add(resultType);
                cache.computeIfAbsent(basic.index(), x -> new LinkedHashMap<>()).put(convertedGenerics, resultType);
            }
            TypeResolvedTypeDef resolved = ast.typeDefs().get(basic.index());
            TypeDef instantiated;
            if (resolved.nested()) { //If the type we're instantiating is nested, then prepend the current TypeGenerics.
                var prependedGenerics = new ArrayList<>(typeGenerics);
                prependedGenerics.addAll(convertedGenerics);
                instantiated = resolved.instantiate(resultType, this, basic.index(), prependedGenerics, instantiationLoc, cause);
            } else {
                instantiated = resolved.instantiate(resultType, this, basic.index(), convertedGenerics, instantiationLoc, cause);
            }

            resultType.fill(instantiated);
            //And return
            return resultType;
        } else if (resolvedType instanceof ResolvedType.Generic generic) {
            if (generic.isMethod()) {
                return methodGenerics.get(generic.index());
            } else
                return typeGenerics.get(generic.index());
        } else if (resolvedType instanceof ResolvedType.Tuple tuple) {
            List<TypeDef> convertedGenerics = ListUtils.map(tuple.elements(), e -> getOrInstantiate(e, typeGenerics, methodGenerics, instantiationLoc, cause));
            return getTuple(convertedGenerics);
        } else if (resolvedType instanceof ResolvedType.Func func) {
            List<TypeDef> convertedParams = ListUtils.map(func.paramTypes(), p -> getOrInstantiate(p, typeGenerics, methodGenerics, instantiationLoc, cause));
            TypeDef convertedResult = getOrInstantiate(func.resultType(), typeGenerics, methodGenerics, instantiationLoc, cause);
            return getFunc(convertedParams, convertedResult);
        } else {
            throw new IllegalStateException("Unexpected ResolvedType; bug in compiler, please report!");
        }
    }

    public TypeDef getTuple(List<TypeDef> typeDefs) {
        return tupleCache.computeIfAbsent(typeDefs, t -> {
            TypeDef res = new TupleTypeDef(t);
            if (!ListUtils.any(res.generics(), TypeInferenceContext::containsUnknownGeneric))
                allTypeDefs.add(res);
            return res;
        });
    }

    public TypeDef getFunc(List<TypeDef> paramTypes, TypeDef resultType) {
        return funcCache.computeIfAbsent(paramTypes, unused -> new HashMap<>()).computeIfAbsent(resultType, unused -> {
            TypeDef res = new FuncTypeDef(this, paramTypes, resultType);
            if (!ListUtils.any(res.generics(), TypeInferenceContext::containsUnknownGeneric))
                allTypeDefs.add(res);
            return res;
        });
    }

    public TypeDef getBasicBuiltin(BuiltinType type) {
        return getOrInstantiate(new ResolvedType.Basic(ast.builtinIds().get(type), List.of()), List.of(), List.of(), null, null);
    }

    public TypeDef getReflectedBuiltin(Class<?> clazz) {
        return getBasicBuiltin(ast.reflectedBuiltins().get(clazz));
    }

    public TypeDef getGenericBuiltin(BuiltinType type, List<TypeDef> convertedGenerics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
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
        cache.computeIfAbsent(index, x -> new LinkedHashMap<>()).put(convertedGenerics, resultType);
        TypeDef instantiated = ast.typeDefs().get(index).instantiate(resultType, this, index, convertedGenerics, instantiationLoc, cause);
        resultType.fill(instantiated);
        //And return
        return resultType;
    }

    public void registerTypeDef(TypeDef completedTypeDef) {
        allTypeDefs.add(completedTypeDef);
    }


    /**
     * Method to fully convert a TypeResolvedAST into a TypedAST.
     */
    public static TypedAST type(BuiltinTypes builtinTypes, TypeResolvedAST resolvedAST) throws CompilationException {
        //Create the checker
        TypeChecker checker = new TypeChecker(resolvedAST);
        //Type check all the top-level code
        Map<String, TypedFile> typedFiles = MapUtils.mapValues(resolvedAST.files(), file -> {
            checker.pushNewEnv(false, null);
            for (String autoImport : builtinTypes.getAutoImports())
                checker.importExtensionMethods(autoImport, false, null);
            TypedFile res;
            try {
                res = file.type(checker);
            } catch (CompilationException ce) {
                checker.popEnv();
                throw ce;
            }
            checker.popEnv();
            return res;
        });
        //Type check the method bodies, repeatedly, until there are no more
        //NOTE: This cannot be an enhanced for loop! Because:
        //while we check method bodies, *more checked type defs can be added to the list*.
        //If we used an enhanced loop, this would lead to concurrent modification exceptions.
        //However, this way, since new topLevelTypes are always appended to the end, we continue
        //checking method bodies until no new method bodies are added to check, and we reach
        //the end of the list.
        for (int i = 0; i < checker.allTypeDefs.size(); i++)
            checker.allTypeDefs.get(i).checkCode();
        //Return the result
        return new TypedAST(checker.allTypeDefs, typedFiles);
    }


    private record BestMethodFinder(TypeChecker checker, Loc loc, TypeDef currentType, TypeResolvedExpr typeResolvedReceiver, TypedExpr typedReceiver, TypeDef receiverType, String methodName, ArrayList<TypeResolvedExpr> args, List<TypeDef> genericArgs, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, boolean isCallStatic, boolean isSuperCall, TypeDef expectedReturnType, TypeDef.InstantiationStackFrame cause,
                                    //First step: find a list of potentially matching methods we can call.
                                    ArrayList<MethodDef> matchingMethods,
                                    //The cached typed args of the matching methods
                                    ArrayList<List<TypedExpr>> matchingMethodsTypedArgs,
                                    //Create a cache for the check() lookups.
                                    //null map elements mean that we already tried check()ing this, and it failed.
                                    List<IdentityHashMap<TypeDef, TypedExpr>> checkCache,
                                    Mutable<Boolean> foundCheckError,
                                    Mutable<CompilationException> onlyCheckError,
                                    ArrayList<MethodDef> thrownOutDueToWrongReturnType) {

        private BestMethodFinder(TypeChecker checker, Loc loc, TypeDef currentType, TypeResolvedExpr typeResolvedReceiver, TypedExpr typedReceiver, TypeDef receiverType, String methodName, List<TypeResolvedExpr> args, List<TypeDef> genericArgs, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, boolean isCallStatic, boolean isSuperCall, TypeDef expectedReturnType, TypeDef.InstantiationStackFrame cause) {
            this(checker, loc, currentType, typeResolvedReceiver, typedReceiver, receiverType, methodName, new ArrayList<>(args), genericArgs, typeGenerics, methodGenerics, isCallStatic, isSuperCall, expectedReturnType, cause,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(args.size()),
                    new Mutable<>(false),
                    new Mutable<>(),
                    new ArrayList<>());
            for (int i = 0; i < args.size(); i++) checkCache.add(new IdentityHashMap<>());
            boolean special = isCallStatic || isSuperCall || methodName.equals("new");
            if (special != (typeResolvedReceiver == null) || (typeResolvedReceiver == null) != (typedReceiver == null))
                throw new IllegalStateException("Non-special call should always pass a typeResolvedReceiver to BestMethodFinder, and vice versa? bug in compiler, please report!");
        }

        public BestMethodInfo getBestMethod() throws CompilationException {
            //Mutable
            TypeDef receiverType = receiverType();

            //Get the list of all methods on the type to look through
            List<MethodDef> methodsToCheck;
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

            //Is this a "special" call?
            boolean special = isCallStatic || isSuperCall || methodName.equals("new");

            BestMethodInfo output;
            //Loop over all these methods. Non-generic methods have priority:
            checkMethods(ListUtils.filter(methodsToCheck, m -> m.numGenerics() == 0), false);
            output = getResultingBestMethodInfo(false);
            if (output != null) return output;
            //Then generic methods.
            checkMethods(ListUtils.filter(methodsToCheck, m -> m.numGenerics() != 0), false);
            output = getResultingBestMethodInfo(special); //If the call is special, don't look for extension methods
            if (output != null) return output;
            //Finally, extension methods. Call was non-special.
            //We pass true as the second parameter, and checkMethods() handles the differences itself.
            List<MethodDef> extensions = ListUtils.join(ListUtils.join(checker.extensionMethods));
            checkMethods(extensions, true);
            output = getResultingBestMethodInfo(true); //we should error now if nothing was found
            if (output != null) return output;

            throw new IllegalStateException("Should never happen");
        }

        private void checkMethods(List<MethodDef> methodsToCheck, boolean isSearchingExtensions) throws CompilationException {
            //Enable re-assignment
            List<TypeDef> genericArgs = this.genericArgs;
            boolean isCallStatic = this.isCallStatic;
            TypeDef receiverType = this.receiverType;

            //If we're searching extension methods, then we do a little trickery to change what's going on here:
            if (isSearchingExtensions) {
                //Add typeResolvedReceiver as the first arg
                args.add(0, typeResolvedReceiver);
                //Make the call static
                isCallStatic = true;
                //Make the typeResolvedReceiver type be the "Extension methods" type
                receiverType = checker.getBasicBuiltin(ExtensionMethods.INSTANCE);
            }

            //Begin loop
            outer:
            for (MethodDef def : methodsToCheck) {
                //Filter out ones which clearly don't match
                if (!def.name().equals(methodName)) continue; //Name doesn't match
                if (def.isStatic() != isCallStatic) continue; //static vs non-static
                if (def.numParams() != args.size()) continue; //Arity doesn't match

                if (!def.pub()) {
                    if (def instanceof SnuggleMethodDef snuggleMethodDef) {
                        if (!snuggleMethodDef.loc().fileName().equals(loc.fileName()))
                            continue;
                    } else
                        throw new IllegalStateException("Method defs aside from SnuggleMethodDef should always be pub! Bug in compiler, please report!");
                }

                if (def.numGenerics() > 0 && genericArgs.size() != 0 && genericArgs.size() != def.numGenerics()) continue;

                if (def.numGenerics() > 0) {
                    if (def instanceof SnuggleMethodDef snuggleDef) {
                        //We might be trying to use inference, so let's try it:
                        boolean wasInferred = genericArgs.size() == 0;
                        if (genericArgs.size() == 0) {
                            //No explicit generic args. Let's go for inference.
                            TypeInferenceContext ctx = new TypeInferenceContext(def.numGenerics(), loc, methodName, cause);
                            try {
                                //Attempt to infer and set the generic args.
                                genericArgs = ctx.inferGenericArgs(
                                        checker,
                                        currentType,
                                        args,
                                        typeGenerics,
                                        methodGenerics,
                                        expectedReturnType,
                                        snuggleDef.paramTypeGetter(),
                                        snuggleDef.returnTypeGetter()
                                );
                            } catch (TypeCheckingException e) {
                                //If it fails, then this method doesn't work. Continue.
                                continue;
                            }
                        }

                        //If it succeeded, then the generic args have been set, and we proceed as normal.
                        boolean isNew = !snuggleDef.hasInstantiated(genericArgs);
                        def = snuggleDef.instantiate(genericArgs);
                        if (wasInferred)
                            try {
                                def.checkCode();
                            } catch (CompilationException e) {
                                if (!foundCheckError.v || onlyCheckError.v != null && e.getMessage().equals(onlyCheckError.v.getMessage())) {
                                    foundCheckError.v = true;
                                    onlyCheckError.v = e;
                                } else {
                                    onlyCheckError.v = null;
                                }
                                continue;
                            }
                        if (isNew)
                            receiverType.addMethod(def); //add a new method, the instantiated one
                    } else {
                        throw new IllegalStateException("Only snuggle method defs can be generic? Bug in compiler, please report!");
                    }
                }

                //Now that we've filtered out those which obviously don't match, move on to less obvious situations.
                //First, invalid return topLevelTypes we can filter out:
                if (expectedReturnType != null) {
                    //We have an expected type, so skip anything whose return type doesn't fit it
                    if (!def.returnType().isSubtype(expectedReturnType)) {
                        thrownOutDueToWrongReturnType.add(def);
                        continue;
                    }
                }

                //Now, let's check the arg topLevelTypes:
                List<TypedExpr> typedArgs = new ArrayList<>(args.size());
                //IF SEARCHING EXTENSIONS, ADD TYPED RECEIVER AS FIRST ARG
                if (isSearchingExtensions) typedArgs.add(typedReceiver);

                int i = 0;
                TypeDef expectedParamType = null;
                try {
                    //Iterate over the expected params.
                    //NOTE: IF SEARCHING EXTENSIONS, THEN START i AT 1 INSTEAD TO SKIP THE FAKE FIRST PARAM
                    for (i = isSearchingExtensions ? 1 : 0; i < def.paramTypes().size(); i++) {
                        //Get the current element of the check cache.
                        //IF SEARCHING EXTENSIONS, SUBTRACT 1 TO OFFSET THE FAKE FIRST PARAM
                        IdentityHashMap<TypeDef, TypedExpr> currentParamCache = checkCache.get(isSearchingExtensions ? i - 1 : i);

                        //Get the expected param type
                        expectedParamType = def.paramTypes().get(i);
                        //check() the arg against the param.
                        //If we've already check()ed the same arg with the same param before,
                        //then we can reuse that.
                        TypedExpr typedArg = currentParamCache.get(expectedParamType);
                        if (typedArg == null) {
                            if (currentParamCache.containsKey(expectedParamType)) {
                                //We already tried calling check() on this, and it failed. Continue.
                                continue outer;
                            }
                            //We haven't checked this arg with this type yet, so do it now:
                            typedArg = args.get(i).check(currentType, checker, typeGenerics, methodGenerics, expectedParamType, cause);
                            //If that didn't throw an exception, then add it to the cache
                            currentParamCache.put(expectedParamType, typedArg);
                        }
                        //Add it to the list of typed args
                        typedArgs.add(typedArg);
                    }
                } catch (CompilationException e) {
                    //A type check failed, so this method def is not applicable.
                    //Note this information down and continue.
                    checkCache.get(isSearchingExtensions ? i - 1 : i).put(expectedParamType, null);
                    if (!foundCheckError.v || onlyCheckError.v != null && e.getMessage().equals(onlyCheckError.v.getMessage())) {
                        foundCheckError.v = true;
                        onlyCheckError.v = e;
                    } else {
                        onlyCheckError.v = null;
                    }
                    continue;
                }

                //All the checks succeeded, so let's add this to our lists:
                matchingMethods.add(def);
                matchingMethodsTypedArgs.add(isSearchingExtensions ? typedArgs.subList(1, typedArgs.size()) : typedArgs);
            }
        }

        private BestMethodInfo getResultingBestMethodInfo(boolean allDone) throws CompilationException {
            //Multiple methods matched:
            if (matchingMethods.size() > 1) {
                //If there are multiple matching methods, try choosing the most specific one.
                //If there is no most specific one, the following errors.
                int bestIndex = tryChoosingMostSpecific(loc, methodName, matchingMethods, cause);
                return new BestMethodInfo(receiverType, matchingMethods.get(bestIndex), matchingMethodsTypedArgs.get(bestIndex));
            }

            //No methods matched:
            if (matchingMethods.size() == 0 && allDone) {
                //If there are no matching methods, error since we couldn't find any applicable one.
                if (foundCheckError.v && onlyCheckError.v != null) {
                    //In this case, throw the only check error we ran into:
                    throw onlyCheckError.v;
                } else if (thrownOutDueToWrongReturnType.size() > 0) {
                    //Need to look through the methods that were thrown out due to wrong return type, and use this
                    //info to craft a nice error message for this situation.
                    StringBuilder previouslyMatchingReturnTypes = new StringBuilder();
                    for (MethodDef thrownOut : thrownOutDueToWrongReturnType) {
                        try {
                            //Iterate over the expected params and check() them all
                            for (int i = 0; i < thrownOut.paramTypes().size(); i++) {
                                TypeDef expectedParamType = thrownOut.paramTypes().get(i);
                                args.get(i).check(currentType, checker, typeGenerics, methodGenerics, expectedParamType, cause);
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
                        throw new TypeCheckingException("Expected method \"" + methodName + "\" to return " + expectedTypeName + ", but only found options " + previouslyMatchingReturnTypes, loc, cause);
                    }
                }
                throw new NoSuitableMethodException(methodName, isCallStatic, isSuperCall, receiverType, loc, cause);
            } else if (matchingMethods.size() == 1) {
                //Exactly one method matched, so we're good
                return new BestMethodInfo(receiverType, matchingMethods.get(0), matchingMethodsTypedArgs.get(0));
            }
            return null;
        }

        /**
         * Attempt to choose the most specific method from the given list of methods.
         * If there is no most specific, throws a TooManyMethodsException.
         * The return value is the index of the most specific method.
         */
        private static int tryChoosingMostSpecific(Loc loc, String methodName, List<MethodDef> matchingMethods, TypeDef.InstantiationStackFrame cause) throws CompilationException {
            //Create the indices list
            List<Integer> arr = new ArrayList<>(matchingMethods.size());
            for (int i = 0; i < matchingMethods.size(); i++)
                arr.add(i);

            //Sort, so the most specific wind up at the front
            ListUtils.insertionSort(arr, (a, b) -> matchingMethods.get(a).compareSpecificity(matchingMethods.get(b)));

            //If the first 2 are equally specific, error
            if (matchingMethods.get(arr.get(0)).compareSpecificity(matchingMethods.get(arr.get(1))) == 0)
                throw new TooManyMethodsException(methodName, loc, cause);
            return arr.get(0);
        }
    }

    /**
     * Helper method to deal with method overload resolution!
     * <p>
     * Tries to find the best method and return useful info about it.
     * If no best method can be chosen, instead throws a CompilationException.
     *
     * @param typeResolvedReceiver  If this is a regular instance method call, then the typed receiver of the method.
     *                              If the call is static, super, or a constructor, then it's null.
     * @param typedReceiver         If this is a regular instance method call, then the typed receiver of the method.
     *                              If the call is static, super, or a constructor, then it's null.
     * @param receiverType          The type of the typeResolvedReceiver. This is what we look up methods on.
     * @param methodName            The name in the method call.
     * @param args                  The TypeResolved arguments given to the method call. They will be checked here.
     * @param genericArgs           The generic arguments to the method call.
     * @param typeGenerics          The instantiating generic topLevelTypes that are passed to TypedExpr's check() and infer() methods.
     * @param isCallStatic          Whether the call was made statically.
     * @param isSuperCall           Whether this call to a method was made using the super keyword. If it was, then there's a
     *                              special process for selecting eligible methods.
     * @param expectedReturnType    The type which the best method must return. If null, there's no restriction on the return type.
     * @param cause                 The stack frame for the cause of the instantiation of new topLevelTypes while checking a method.
     */
    public BestMethodInfo getBestMethod(Loc loc, TypeDef currentType, TypeResolvedExpr typeResolvedReceiver, TypedExpr typedReceiver, TypeDef receiverType, String methodName, List<TypeResolvedExpr> args, List<TypeDef> genericArgs, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, boolean isCallStatic, boolean isSuperCall, TypeDef expectedReturnType, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        return new BestMethodFinder(this, loc, currentType, typeResolvedReceiver, typedReceiver, receiverType, methodName, args, genericArgs, typeGenerics, methodGenerics, isCallStatic, isSuperCall, expectedReturnType, cause).getBestMethod();
    }

    //Holds the result of getBestMethod().
    //Contains all the information needed for the caller; particularly
    //the method def that was chosen as well as the type-checked arguments.
    public record BestMethodInfo(TypeDef receiverType, MethodDef methodDef, List<TypedExpr> typedArgs) {}

    //Get the common supertype among all types in the set
    //If there is no common supertype, returns null
    public static TypeDef getCommonSupertype(Set<TypeDef> allTypes) throws CompilationException {
        if (allTypes.size() == 0)
            return null;
        Iterator<TypeDef> iter = allTypes.iterator();
        TypeDef top = iter.next();
        while (iter.hasNext()) {
            TypeDef t = iter.next();
            if (t.isSubtype(top)) continue;
            else if (top.isSubtype(t)) top = t;
            else return null;
        }
        return top;
    }

}
