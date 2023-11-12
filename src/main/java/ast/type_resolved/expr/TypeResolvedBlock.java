package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.def.field.TypeResolvedFieldDef;
import ast.typed.def.type.TypeDef;
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
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //empty block {} just evaluates to unit
        if (exprs.size() == 0)
            return new TypedLiteral(cause, loc, Unit.INSTANCE, checker.getBasicBuiltin(UnitType.INSTANCE));

        //Otherwise, map all exprs to inferred exprs, in a pushed checker env
        checker.push();
        List<TypedExpr> inferredExprs = ListUtils.map(exprs, e -> e.infer(currentType, checker, typeGenerics, methodGenerics, cause));
        checker.pop();
        //Result type is the type of the last expr in the block
        return new TypedBlock(loc, inferredExprs, inferredExprs.get(inferredExprs.size() - 1).type());
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Empty block case
        if (exprs.size() == 0) {
            if (!checker.getBasicBuiltin(UnitType.INSTANCE).isSubtype(expected))
                throw new TypeCheckingException(expected, "block expression", checker.getBasicBuiltin(UnitType.INSTANCE), loc, cause);
            else
                return new TypedLiteral(cause, loc, Unit.INSTANCE, checker.getBasicBuiltin(UnitType.INSTANCE));
        }
        //Otherwise, infer all exprs except the last one, which is checked instead.
        List<TypedExpr> typedExprs = new ArrayList<>(exprs.size());
        checker.push();
        for (int i = 0; i < exprs.size(); i++) {
            if (i == exprs.size() - 1)
                typedExprs.add(exprs.get(i).check(currentType, checker, typeGenerics, methodGenerics, expected, cause));
            else
                typedExprs.add(exprs.get(i).infer(currentType, checker, typeGenerics, methodGenerics, cause));
        }
        checker.pop();
        return new TypedBlock(loc, typedExprs, expected);
    }
}
