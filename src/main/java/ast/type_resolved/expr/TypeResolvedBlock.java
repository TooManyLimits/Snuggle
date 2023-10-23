package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.Type;
import ast.typed.expr.TypedBlock;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import builtin_types.types.UnitType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import runtime.Unit;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;

public record TypeResolvedBlock(Loc loc, List<TypeResolvedExpr> exprs) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        for (TypeResolvedExpr e : exprs)
            e.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(Type currentType, TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        //empty block {} just evaluates to unit
        if (exprs.size() == 0)
            return new TypedLiteral(loc, Unit.INSTANCE, checker.pool().getBasicBuiltin(UnitType.INSTANCE));

        //Otherwise, map all exprs to inferred exprs, in a pushed checker env
        checker.push();
        List<TypedExpr> inferredExprs = ListUtils.map(exprs, e -> e.infer(currentType, checker, typeGenerics));
        checker.pop();
        //Result type is the type of the last expr in the block
        return new TypedBlock(loc, inferredExprs, inferredExprs.get(inferredExprs.size() - 1).type());
    }

    @Override
    public TypedExpr check(Type currentType, TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        //Empty block case
        if (exprs.size() == 0) {
            if (!checker.pool().getBasicBuiltin(UnitType.INSTANCE).isSubtype(expected, checker.pool()))
                throw new TypeCheckingException("Expected " + expected.name(checker.pool()) + ", but got unit", loc);
            else
                return new TypedLiteral(loc, Unit.INSTANCE, checker.pool().getBasicBuiltin(UnitType.INSTANCE));
        }
        //Otherwise, infer all exprs except the last one, which is checked instead.
        List<TypedExpr> typedExprs = new ArrayList<>(exprs.size());
        checker.push();
        for (int i = 0; i < exprs.size(); i++) {
            if (i == exprs.size() - 1)
                typedExprs.add(exprs.get(i).check(currentType, checker, typeGenerics, expected));
            else
                typedExprs.add(exprs.get(i).infer(currentType, checker, typeGenerics));
        }
        checker.pop();
        return new TypedBlock(loc, typedExprs, expected);
    }
}
