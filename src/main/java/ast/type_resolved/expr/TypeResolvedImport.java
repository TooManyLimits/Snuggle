package ast.type_resolved.expr;

import builtin_types.types.BoolType;
import exceptions.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.Type;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedImport;
import builtin_types.types.UnitType;
import exceptions.TypeCheckingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedImport(Loc loc, String fileName) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {

    }

    @Override
    public TypedExpr infer(TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        Type bool = checker.pool().getBasicBuiltin(BoolType.INSTANCE);
        return new TypedImport(loc, fileName, bool);
    }

    @Override
    public TypedExpr check(TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        Type bool = checker.pool().getBasicBuiltin(BoolType.INSTANCE);
        if (!bool.isSubtype(expected, checker.pool()))
            throw new TypeCheckingException("Expected " + expected.name(checker.pool()) + ", got bool", loc);
        return new TypedImport(loc, fileName, bool);
    }
}
