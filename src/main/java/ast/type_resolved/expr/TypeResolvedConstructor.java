package ast.type_resolved.expr;

import ast.typed.def.method.MethodDef;
import exceptions.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.Type;
import ast.typed.expr.TypedConstructor;
import ast.typed.expr.TypedExpr;
import exceptions.NoSuitableMethodException;
import exceptions.TooManyMethodsException;
import exceptions.TypeCheckingException;
import lexing.Loc;

import java.util.ArrayList;
import java.util.List;

public record TypeResolvedConstructor(Loc loc, ResolvedType type, List<TypeResolvedExpr> args) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        verifier.verifyType(type, loc);
        for (TypeResolvedExpr arg : args)
            arg.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
//        //Get the annotatedType
        Type t = checker.pool().getOrInstantiateType(type, typeGenerics);

        //Find methods matching methodName "new"
        List<? extends MethodDef> matchingMethods = TypeResolvedMethodCall.findMatching(t, "new", args, List.of(), checker, typeGenerics, false);

        MethodDef matchingMethod;
        //If there are multiple matching methods, error because we don't know which to call
        if (matchingMethods.size() > 1)
            matchingMethod = TypeResolvedMethodCall.tryChoosingMostSpecific(loc, "new", matchingMethods, checker);
        //If there are no matching methods, error since we couldn't find any applicable one
        else if (matchingMethods.size() == 0)
            throw new NoSuitableMethodException("Unable to find suitable constructor", loc);
        else
            matchingMethod = matchingMethods.get(0);

        //There's only one matching method; return the proper TypedExpr constructor
        //TODO: Store this list of typedArgs in MatchingInfo and fetch; it was already computed
        List<Type> expectedParams = matchingMethod.paramTypes();
        List<TypedExpr> typedArgs = new ArrayList<>(args.size());
        for (int i = 0; i < expectedParams.size(); i++) {
            typedArgs.add(args.get(i).check(checker, typeGenerics, expectedParams.get(i)));
        }

        return new TypedConstructor(loc, t, matchingMethod, typedArgs);
    }

    @Override
    public TypedExpr check(TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        TypedExpr inferred = infer(checker, typeGenerics);
        if (!inferred.type().isSubtype(expected, checker.pool()))
            throw new TypeCheckingException("Expected annotatedType " + expected.name(checker.pool()) + ", got " + inferred.type().name(checker.pool()), loc);
        return inferred;
    }
}
