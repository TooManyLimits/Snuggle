package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.def.field.TypeResolvedFieldDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedAugmentedFieldAssignment;
import ast.typed.expr.TypedExpr;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.util.List;

/**
 * Specifically used in the case where we have a.x += b.
 * Anything else was filtered out at the previous stage and converted into method calls / blocks instead.
 */
public record TypeResolvedAugmentedFieldAssignment(Loc loc, String methodName, String fallback, TypeResolvedExpr lhs, TypeResolvedExpr rhs) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        lhs.verifyGenericArgCounts(verifier);
        rhs.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics) throws CompilationException {
        //Infer type of "a.x"
        TypedExpr typedLhs = lhs.infer(currentType, checker, typeGenerics);
        //Check that "a.x" has a method of name methodName or fallback
        //That accepts rhs as a parameter, and returns the type of a.x
        TypeChecker.BestMethodInfo best = checker.tryMultipleMethodsForBest(loc, currentType, typedLhs.type(), List.of(methodName, fallback), List.of(rhs), List.of(), typeGenerics, false, false, typedLhs.type());
        //Return it
        return new TypedAugmentedFieldAssignment(loc, best.methodDef().delegate(), typedLhs, best.typedArgs().get(0), typedLhs.type());
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected) throws CompilationException {
        TypedExpr inferred = infer(currentType, checker, typeGenerics);
        if (!inferred.type().isSubtype(expected))
            throw new TypeCheckingException("Expected " + expected.name() + ", got " + inferred.type().name(), loc);
        return inferred;
    }
}
