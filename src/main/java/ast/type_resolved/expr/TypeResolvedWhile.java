package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.TypeCheckingHelper;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
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
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Check cond is a bool
        TypedExpr checkedCond = cond.check(currentType, checker, typeGenerics, methodGenerics, checker.getBasicBuiltin(BoolType.INSTANCE), cause);
        //Infer the body
        TypedExpr inferredBody = body.infer(currentType, checker, typeGenerics, methodGenerics, cause);
        //Create an Option<> around the body's inferred type
        TypeDef optionalType = checker.getGenericBuiltin(OptionType.INSTANCE, List.of(inferredBody.type()), loc, cause);
        inferredBody = TypeCheckingHelper.wrapInOption(loc, inferredBody, checker, cause);
        return new TypedWhile(loc, checkedCond, inferredBody, optionalType);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Check cond is a bool
        TypedExpr checkedCond = cond.check(currentType, checker, typeGenerics, methodGenerics, checker.getBasicBuiltin(BoolType.INSTANCE), cause);
        //Ensure that the expected type is an Option
        if (expected.builtin() == OptionType.INSTANCE) {
            //It was an Option, so grab its inner nested type
            TypeDef expectedBodyType = ((BuiltinTypeDef) expected.get()).generics.get(0);
            //And check() the body for that type
            TypedExpr typedBody = body.check(currentType, checker, typeGenerics, methodGenerics, expectedBodyType, cause);
            //Wrap the body in an option
            typedBody = TypeCheckingHelper.wrapInOption(loc, typedBody, checker, cause);
            //Return the result
            return new TypedWhile(loc, checkedCond, typedBody, expected);
        } else {
            throw new TypeCheckingException("Expected type \"" + expected.name() + "\", but loop returns Option", loc, cause);
        }
    }
}
