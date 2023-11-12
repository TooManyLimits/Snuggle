package ast.type_resolved.expr;

import ast.type_resolved.def.field.TypeResolvedFieldDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.numbers.FloatLiteralType;
import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntLiteralType;
import builtin_types.types.numbers.IntegerType;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.Fraction;
import util.IntLiteralData;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * TypeResolvedLiteral value is same as ParsedLiteral.
 */
public record TypeResolvedLiteral(Loc loc, Object value, ResolvedType resolved) implements TypeResolvedExpr {
    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        //No need to do anything
    }

    @Override
    public TypedLiteral infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        Object newValue = (value instanceof IntLiteralData data) ? data.value() : value;
        return new TypedLiteral(cause, loc, newValue, checker.getOrInstantiate(resolved, List.of(), List.of(), loc, cause));
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedLiteral e = infer(currentType, checker, typeGenerics, methodGenerics, cause);
        TypeDef intLiteralType = checker.getBasicBuiltin(IntLiteralType.INSTANCE);
        TypeDef floatLiteralType = checker.getBasicBuiltin(FloatLiteralType.INSTANCE);
        if (e.type().equals(intLiteralType)) {
            //Check int
            BigInteger value = (BigInteger) e.obj();

            //Expected int literal? Cool!
            if (expected.equals(intLiteralType))
                return e;

            //Expected other int type?
            for (IntegerType t : IntegerType.ALL_INT_TYPES)
                if (checker.getBasicBuiltin(t).equals(expected))
                    return new TypedLiteral(cause, e.loc(), value, expected);

            //Expected float type?
            for (FloatType t : FloatType.ALL_FLOAT_TYPES)
                if (checker.getBasicBuiltin(t).equals(expected))
                    return new TypedLiteral(cause, e.loc(), new Fraction(value, BigInteger.ONE), expected);

            //Didn't expect any of those numeric topLevelTypes? Error
            throw new TypeCheckingException("Expected " + expected.name() + ", got int literal", loc, cause);
        } else if (e.type().equals(floatLiteralType)) {
            //Practically same structure as above IntLiteral version ^
            if (expected.equals(floatLiteralType))
                return e;
            for (FloatType t : FloatType.ALL_FLOAT_TYPES)
                if (checker.getBasicBuiltin(t).equals(expected))
                    return new TypedLiteral(cause, e.loc(), value, expected);
            throw new TypeCheckingException("Expected " + expected.name() + ", got float literal", loc, cause);
        }
        if (!e.type().isSubtype(expected))
            throw new TypeCheckingException("Expected " + expected.name() + ", got " + e.type().name() + " literal", loc, cause);
        return e;
    }

}
