package ast.type_resolved.expr;

import ast.type_resolved.def.field.TypeResolvedFieldDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.BoolType;
import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedImport;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedImport(Loc loc, String fileName) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {

    }

    @Override
    public TypedImport infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics) throws CompilationException {
        TypeDef bool = checker.getBasicBuiltin(BoolType.INSTANCE);
        return new TypedImport(loc, fileName, bool);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected) throws CompilationException {
        TypeDef bool = checker.getBasicBuiltin(BoolType.INSTANCE);
        if (!bool.isSubtype(expected))
            throw new TypeCheckingException("Expected " + expected.name() + ", got bool", loc);
        return new TypedImport(loc, fileName, bool);
    }
}
