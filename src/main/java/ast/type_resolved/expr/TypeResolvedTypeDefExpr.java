package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedTypeDefExpr;
import builtin_types.types.UnitType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedTypeDefExpr(Loc loc, ResolvedType.Basic basicTypeHandle) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        //Theoretically, the type def(s) defined by this should have already been verified earlier on.
        //So we shouldn't need to do anything here.
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypeDef unitType = checker.getBasicBuiltin(UnitType.INSTANCE);
        return new TypedTypeDefExpr(cause, loc, () -> checker.getAllInstantiated(basicTypeHandle), unitType);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr typed = infer(currentType, checker, typeGenerics, methodGenerics, cause);
        if (!typed.type().isSubtype(expected))
            throw new TypeCheckingException(expected, "Type definition", typed.type(), loc, cause);
        return typed;
    }
}
