package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedWhile;
import builtin_types.types.BoolType;
import builtin_types.types.OptionType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedWhile(Loc loc, TypeResolvedExpr cond, TypeResolvedExpr body) implements TypeResolvedExpr {
    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        cond.verifyGenericArgCounts(verifier);
        body.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(Type currentType, TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        //Check cond is a bool
        TypedExpr checkedCond = cond.check(currentType, checker, typeGenerics, checker.pool().getBasicBuiltin(BoolType.INSTANCE));
        //Infer the body
        TypedExpr inferredBody = body.infer(currentType, checker, typeGenerics);
        //Create an Option<> around the body's inferred type
        Type optionalType = checker.pool().getGenericBuiltin(OptionType.INSTANCE, List.of(inferredBody.type()));
        return new TypedWhile(loc, checkedCond, inferredBody, optionalType);
    }

    @Override
    public TypedExpr check(Type currentType, TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        //Check cond is a bool
        TypedExpr checkedCond = cond.check(currentType, checker, typeGenerics, checker.pool().getBasicBuiltin(BoolType.INSTANCE));
        //Ensure that the expected type is an Option
        if (checker.pool().getTypeDef(expected) instanceof BuiltinTypeDef b && b.builtin() == OptionType.INSTANCE) {
            //It was an Option, so grab its inner nested type
            Type expectedBodyType = b.generics().get(0);
            //And check() the body for that type
            TypedExpr typedBody = body.check(currentType, checker, typeGenerics, expectedBodyType);
            //Return the result
            return new TypedWhile(loc, checkedCond, typedBody, expected);
        } else {
            throw new TypeCheckingException("Expected " + expected.name(checker.pool()) + ", but while loop returns Option", loc);
        }
    }
}
