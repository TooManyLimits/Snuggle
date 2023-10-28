package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedFieldAccess;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import exceptions.compile_time.UndeclaredVariableException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

public record TypeResolvedFieldAccess(Loc loc, TypeResolvedExpr lhs, String name) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        lhs.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics) throws CompilationException {
        TypedExpr typedLhs = lhs.infer(currentType, checker, typeGenerics);
        FieldDef field = ListUtils.find(typedLhs.type().fields(), f -> f.name().equals(name));
        if (field == null)
            throw new UndeclaredVariableException("Unable to locate field \"" + name + "\"", loc);
        return new TypedFieldAccess(loc, typedLhs, field, field.type());
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected) throws CompilationException {
        TypedExpr e = infer(currentType, checker, typeGenerics);
        if (!e.type().isSubtype(expected))
            throw new TypeCheckingException("Expected type " + expected.name() + ", but field \"" + name + "\" has type " + e.type().name(), loc);
        return e;
    }
}
