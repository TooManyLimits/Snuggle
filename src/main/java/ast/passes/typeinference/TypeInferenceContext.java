package ast.passes.typeinference;

import ast.passes.TypeChecker;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedLambda;
import ast.typed.def.type.FromTypeHead;
import ast.typed.def.type.FuncTypeDef;
import ast.typed.def.type.TupleTypeDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.LateInitFunction;
import util.ListUtils;

import java.util.List;

/**
 * These work very localized, create one whenever
 * we want to do a bit of type inference.
 * Like java, type inference is only done when
 * trying to figure out method generics. This is
 * to try to keep things simple.
 */
public class TypeInferenceContext {

    //The unknowns. All start as null, but once
    //they're filled in, then we're done :>
    private final List<TypeDef> unknownMethodGenerics;
    private final Loc loc;
    private final String methodName;
    private final TypeDef.InstantiationStackFrame cause;

    public TypeInferenceContext(int numMethodGenerics, Loc loc, String methodName, TypeDef.InstantiationStackFrame cause) {
        this.unknownMethodGenerics = ListUtils.generate(numMethodGenerics, UnknownGenericType::new);
        this.loc = loc;
        this.methodName = methodName;
        this.cause = cause;
    }

    private int numGenerics() {
        return unknownMethodGenerics.size();
    }

    private boolean done() {
        //We're done if none of the method generics are UnknownGenericType.
        return !ListUtils.any(unknownMethodGenerics, x -> x instanceof UnknownGenericType);
    }

    //Attempt to infer the generic arguments to the method call, given the call site
    //If inference fails, throw an error. The caller should handle it.
    public List<TypeDef> inferGenericArgs(
            TypeChecker checker,
            TypeDef currentType,
            List<TypeResolvedExpr> args,
            List<TypeDef> typeGenerics,
            List<TypeDef> curMethodGenerics,
            TypeDef expectedReturnType,
            LateInitFunction<List<TypeDef>, List<TypeDef>, RuntimeException> paramTypeGetter,
            LateInitFunction<List<TypeDef>, TypeDef, RuntimeException> returnTypeGetter
    ) throws CompilationException {

        //Get the param and return types
        List<TypeDef> paramTypes = paramTypeGetter.get(unknownMethodGenerics); //may contain UnknownGenericType
        TypeDef resultType = returnTypeGetter.get(unknownMethodGenerics); //may contain UnknownGenericType

        //If arity doesn't match, then illegal state - we shouldn't ever have this happen
        if (paramTypes.size() != args.size())
            throw new IllegalStateException("Arity mismatch - but this should never have happened? Bug in compiler, please report!");

        //Create an array to store whether we need to infer each arg or not.
        //If a param makes no mention of any unknown generic, then we don't need to bother inferring its arg.
        boolean[] needToInferArg = new boolean[args.size()];
        boolean needToInferResult = containsUnknownGeneric(resultType);
        //Fill out this array with starting data:
        for (int i = 0; i < args.size(); i++)
            needToInferArg[i] = containsUnknownGeneric(paramTypes.get(i));

        inferenceLoop:
        while (true) {
            //Whether the state changed this iteration
            boolean changed = false;

            //Try return type with expectedReturnType:
            if (needToInferResult && expectedReturnType != null) {
                needToInferResult = false;
                changed = true;
                //If we have an expected return type, try to destructure
                //the output with it, then update stuff
                destructure(expectedReturnType, resultType);
                //And then update param/result types and needToInferArg
                paramTypes = paramTypeGetter.get(unknownMethodGenerics);
                resultType = returnTypeGetter.get(unknownMethodGenerics);
                needToInferResult &= containsUnknownGeneric(resultType);
                for (int j = 0; j < args.size(); j++)
                    needToInferArg[j] &= containsUnknownGeneric(paramTypes.get(j));
            }

            //If done, break out
            if (done()) break inferenceLoop;

            //Try args:
            for (int i = 0; i < args.size(); i++) {
                TypeResolvedExpr arg = args.get(i);

                if (needToInferArg[i] && arg instanceof TypeResolvedLambda lambda) {
                    //Special code for lambdas
                    TypeDef expected = paramTypes.get(i);
                    if (expected instanceof FuncTypeDef funcType) {
                        if (!ListUtils.any(funcType.paramTypes, TypeInferenceContext::containsUnknownGeneric)) {
                            //If none of the params contain an unknown generic, try to infer the type:
                            TypeDef inferred;
                            try {
                                inferred = lambda.inferTypeGivenArgTypes(funcType.paramTypes, currentType, checker, typeGenerics, curMethodGenerics, cause);
                            } catch (TypeCheckingException e) {
                                //If we failed, continue onwards
                                continue;
                            }
                            //Destructure to gain information (modifying the unknownMethodGenerics)
                            destructure(inferred, paramTypes.get(i));
                            //And then update param/result types, and needToInferArg
                            paramTypes = paramTypeGetter.get(unknownMethodGenerics);
                            resultType = returnTypeGetter.get(unknownMethodGenerics);
                            needToInferResult &= containsUnknownGeneric(resultType);
                            for (int j = 0; j < args.size(); j++)
                                needToInferArg[j] &= containsUnknownGeneric(paramTypes.get(j));
                        } else {
                            //If any of the params contain an unknown generic, then inference cannot be done
                            //right now. So let's continue to the next iteration.
                            continue;
                        }
                    } else {
                        //If a func type def wasn't expected, but a lambda
                        //was provided, this is an error, and we cannot infer.
                        throw new TypeCheckingException("Unable to infer generic args to method \"" + methodName + "\"", loc, cause);
                    }
                } else if (needToInferArg[i]) {
                    //Other things, just try to infer() them (if we haven't inferred successfully before)
                    TypeDef inferred;
                    try {
                        inferred = arg.infer(currentType, checker, typeGenerics, unknownMethodGenerics, cause).type();
                        //If we succeeded, then we no longer need to infer the arg, and also note that we changed
                        needToInferArg[i] = false;
                        changed = true;
                    } catch (TypeCheckingException e) {
                        //If we failed, then continue onwards
                        continue;
                    }
                    //Destructure to gain information (modifying the unknownMethodGenerics)
                    destructure(inferred, paramTypes.get(i));
                    //And then update param/result types and needToInferArg
                    paramTypes = paramTypeGetter.get(unknownMethodGenerics);
                    resultType = returnTypeGetter.get(unknownMethodGenerics);
                    needToInferResult &= containsUnknownGeneric(resultType);
                    for (int j = 0; j < args.size(); j++)
                        needToInferArg[j] &= containsUnknownGeneric(paramTypes.get(j));
                }

                //If done, break out
                if (done()) break inferenceLoop;
            }

            //Break if we're done
            if (done()) break inferenceLoop;

            //If we made it a whole iteration without finding anything,
            //then we can't do anything, so break out
            if (!changed) break inferenceLoop;
        }

        //We're now out of the loop. There are two possibilities:
        if (done()) {
            //If we finished because we were done, then report this information.
            return unknownMethodGenerics;
        } else {
            //Otherwise, the type inference couldn't be solved. So error out.
            throw new TypeCheckingException("Unable to infer generic args to method \"" + methodName + "\"", loc, cause);
        }
    }

    //If target is an UnknownGenericType, then save actualType in methodGenerics
    //If target and actualType share the same type head, then destructure them together
    //Otherwise, do nothing.
    private void destructure(TypeDef actualType, TypeDef target) throws CompilationException {
        if (target instanceof UnknownGenericType unknown) {
            int index = unknown.index();
            if (unknownMethodGenerics.get(index) instanceof UnknownGenericType)
                unknownMethodGenerics.set(index, actualType);
            else {
                //There was already a method generic there - if they're different,
                //then it's an error and inference has failed
                if (unknownMethodGenerics.get(index).get() != actualType.get())
                    throw new TypeCheckingException("Unable to infer generic args to method \"" + methodName + "\"", loc, cause);
            }
        } else if (
                actualType.get() instanceof FromTypeHead actualTypeHead &&
                target.get() instanceof FromTypeHead targetTypeHead &&
                actualTypeHead.getTypeHeadId() == targetTypeHead.getTypeHeadId()
                ||
                actualType.get() instanceof FuncTypeDef actualFunc &&
                target.get() instanceof FuncTypeDef targetFunc &&
                actualFunc.paramTypes.size() == targetFunc.paramTypes.size()
                ||
                actualType instanceof TupleTypeDef actualTuple &&
                target instanceof TupleTypeDef targetTuple &&
                actualTuple.elementTypes.size() == targetTuple.elementTypes.size()) {
            //Both are from the same type head, and they are otherwise compatible
            List<TypeDef> actualGenerics = actualType.generics();
            List<TypeDef> targetGenerics = target.generics();
            if (actualGenerics.size() != targetGenerics.size())
                throw new IllegalStateException("Inference failure - wrong # of generics? Bug in compiler, please report!");

            //For each pair of generics in the list, destructure them.
            ListUtils.mapTwo(actualGenerics, targetGenerics, (actualGeneric, targetGeneric) -> {
                destructure(actualGeneric, targetGeneric);
                return null;
            });
        }
        //If neither condition is true, then do nothing.
    }

    //Whether the given typedef contains, somewhere in it, an unknown generic.
    public static boolean containsUnknownGeneric(TypeDef typeDef) {
        //This is an unknown generic, or if any of our contained types are such, recursively
        return (typeDef instanceof UnknownGenericType) ||
                ListUtils.any(typeDef.generics(), TypeInferenceContext::containsUnknownGeneric);
    }

}
