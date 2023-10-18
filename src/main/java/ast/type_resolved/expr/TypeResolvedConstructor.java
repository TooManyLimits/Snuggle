package ast.type_resolved.expr;

import builtin_types.types.UnitType;
import exceptions.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.Type;
import ast.typed.expr.TypedConstructor;
import ast.typed.expr.TypedExpr;
import exceptions.TypeCheckingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedConstructor(Loc loc, ResolvedType type, List<TypeResolvedExpr> args) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        verifier.verifyType(type, loc);
        for (TypeResolvedExpr arg : args)
            arg.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(Type currentType, TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        //Lookup the best method
        Type t = checker.pool().getOrInstantiateType(type, typeGenerics);
        TypeChecker.BestMethodInfo best = checker.getBestMethod(loc, currentType, t, "new", args, List.of(), typeGenerics, false, false, checker.pool().getBasicBuiltin(UnitType.INSTANCE));
        return new TypedConstructor(loc, t, best.methodDef(), best.typedArgs());
    }

    @Override
    public TypedExpr check(Type currentType, TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        TypedExpr inferred = infer(currentType, checker, typeGenerics);
        if (!inferred.type().isSubtype(expected, checker.pool()))
            throw new TypeCheckingException("Expected type " + expected.name(checker.pool()) + ", got " + inferred.type().name(checker.pool()), loc);
        return inferred;
    }
}
