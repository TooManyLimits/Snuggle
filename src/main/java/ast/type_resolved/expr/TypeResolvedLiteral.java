package ast.type_resolved.expr;

import ast.type_resolved.def.type.BuiltinTypeResolvedTypeDef;
import ast.typed.def.type.BuiltinTypeDef;
import exceptions.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.Type;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntLiteralType;
import builtin_types.types.numbers.IntegerType;
import exceptions.TypeCheckingException;
import lexing.Loc;
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
    public TypedLiteral infer(TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        Object newValue = (value instanceof IntLiteralData data) ? data.value() : value;
        return new TypedLiteral(loc, newValue, checker.pool().getOrInstantiateType(resolved, List.of()));
    }

    @Override
    public TypedExpr check(TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        TypedLiteral e = infer(checker, typeGenerics);
        Type intLiteralType = checker.pool().getBasicBuiltin(IntLiteralType.INSTANCE);
        if (e.type().equals(intLiteralType)) {
            //Check int
            BigInteger value = (BigInteger) e.obj();

            //Expected int literal? Cool!
            if (expected.equals(intLiteralType))
                return e;

//            //Expected other int type?
//            if ((checkInteger(checker, IntegerType.I8, value, expected))  ||
//                (checkInteger(checker, IntegerType.I16, value, expected)) ||
//                (checkInteger(checker, IntegerType.I32, value, expected)) ||
//                (checkInteger(checker, IntegerType.I64, value, expected)) ||
//                (checkInteger(checker, IntegerType.U8, value, expected))  ||
//                (checkInteger(checker, IntegerType.U16, value, expected)) ||
//                (checkInteger(checker, IntegerType.U32, value, expected)) ||
//                (checkInteger(checker, IntegerType.U64, value, expected)))
            for (IntegerType t : IntegerType.ALL_INT_TYPES)
                if (checker.pool().getBasicBuiltin(t).equals(expected))
                    return new TypedLiteral(e.loc(), value, expected);

            for (FloatType t : FloatType.ALL_FLOAT_TYPES)
                if (checker.pool().getBasicBuiltin(t).equals(expected))
                    return new TypedLiteral(e.loc(), new BigDecimal(value), expected);

            //Didn't expect any of those integral types? Error
            throw new TypeCheckingException("Expected " + expected.name(checker.pool()) + ", got int literal", loc);
        }
        if (!e.type().isSubtype(expected, checker.pool()))
            throw new TypeCheckingException("Expected " + expected.name(checker.pool()) + ", got " + e.type().name(checker.pool()), loc);
        return e;
    }

    private boolean checkInteger(TypeChecker checker, IntegerType type, BigInteger value, Type expected) throws CompilationException {
        if (checker.pool().getBasicBuiltin(type).equals(expected)) {
            //If it's expected, if this value fits in the needed range, return true. Otherwise error.
            if (!type.fits(value))
                throw new TypeCheckingException("Expected " + expected.name(checker.pool()) + ", but integer literal " + value + " is out of range. (" + type.min + " to " + type.max + ")", loc);
            return true;
        }
        //If not expected, do nothing
        return false;
    }

}
