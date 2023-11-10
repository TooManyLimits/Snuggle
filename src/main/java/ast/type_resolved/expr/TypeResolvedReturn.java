package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedReturn;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedReturn(Loc loc, TypeResolvedExpr rhs) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        rhs.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr typedRhs = rhs.infer(currentType, checker, typeGenerics, cause);
        return new TypedReturn(loc, typedRhs, typedRhs.type());
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr typedRhs = rhs.check(currentType, checker, typeGenerics, expected, cause);
        return new TypedReturn(loc, typedRhs, typedRhs.type());
    }
}
