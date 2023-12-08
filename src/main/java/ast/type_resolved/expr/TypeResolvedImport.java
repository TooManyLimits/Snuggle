package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedImport;
import builtin_types.types.primitive.BoolType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedImport(Loc loc, String fileName) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {

    }

    @Override
    public TypedImport infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        checker.importExtensionMethods(fileName, false, cause);
        TypeDef bool = checker.getBasicBuiltin(BoolType.INSTANCE);
        return new TypedImport(loc, fileName, bool);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypeDef bool = checker.getBasicBuiltin(BoolType.INSTANCE);
        if (!bool.isSubtype(expected))
            throw new TypeCheckingException(expected, "import expression", checker.getBasicBuiltin(BoolType.INSTANCE), loc, cause);
        return new TypedImport(loc, fileName, bool);
    }
}
