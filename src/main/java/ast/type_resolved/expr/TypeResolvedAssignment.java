package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.def.field.TypeResolvedFieldDef;
import ast.typed.def.type.TypeDef;
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
    public TypedAssignment infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr typedLhs = lhs.infer(currentType, checker, typeGenerics, methodGenerics, cause);
        TypeDef type = typedLhs.type();
        TypedExpr typedRhs = rhs.check(currentType, checker, typeGenerics, methodGenerics, type, cause);
        return new TypedAssignment(cause, loc, typedLhs, typedRhs, type);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedAssignment inferred = infer(currentType, checker, typeGenerics, methodGenerics, cause);
        if (!inferred.type().isSubtype(expected))
            throw new TypeCheckingException(expected, "assignment", inferred.type(), loc, cause);
        return inferred;
    }
}
