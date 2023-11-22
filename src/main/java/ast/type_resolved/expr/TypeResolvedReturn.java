package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedReturn;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.ParsingException;
import lexing.Loc;
import util.LateInit;

import java.util.List;

public record TypeResolvedReturn(Loc loc, TypeResolvedExpr rhs) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        rhs.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr typedRhs = rhs.infer(currentType, checker, typeGenerics, methodGenerics, cause);
        return new TypedReturn(loc, typedRhs, typedRhs.type());
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        LateInit<TypeDef, CompilationException> returnTypeGetter = checker.getDesiredReturnType();
        if (returnTypeGetter == null)
            throw new ParsingException("Cannot return here - can only return inside a function", loc);
        TypeDef desiredReturnType = returnTypeGetter.get();
        TypedExpr typedRhs;
        if (desiredReturnType == null) {
            typedRhs = rhs.infer(currentType, checker, typeGenerics, methodGenerics, cause);
            checker.getAttemptedReturnTypes().add(typedRhs.type().get());
        } else {
            typedRhs = rhs.check(currentType, checker, typeGenerics, methodGenerics, desiredReturnType, cause);
        }
        return new TypedReturn(loc, typedRhs, expected); //always matches expected
    }
}
