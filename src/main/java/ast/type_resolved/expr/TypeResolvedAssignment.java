package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.Type;
import ast.typed.expr.TypedAssignment;
import ast.typed.expr.TypedExpr;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedAssignment(Loc loc, TypeResolvedExpr lhs, TypeResolvedExpr rhs) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        lhs.verifyGenericArgCounts(verifier);
        rhs.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedAssignment infer(Type currentType, TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        TypedExpr typedLhs = lhs.infer(currentType, checker, typeGenerics);
        Type type = typedLhs.type();
        TypedExpr typedRhs = rhs.check(currentType, checker, typeGenerics, type);
        return new TypedAssignment(loc, typedLhs, typedRhs, type);
    }

    @Override
    public TypedExpr check(Type currentType, TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        TypedAssignment inferred = infer(currentType, checker, typeGenerics);
        if (!inferred.type().isSubtype(expected, checker.pool()))
            throw new TypeCheckingException("Expected " + expected.name(checker.pool()) + ", got " + inferred.type().name(checker.pool()), loc);
        return inferred;
    }
}
