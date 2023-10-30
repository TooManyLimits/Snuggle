package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedCast;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import builtin_types.types.OptionType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.math.BigInteger;
import java.util.List;

public record TypeResolvedCast(Loc loc, int tokenLine, TypeResolvedExpr lhs, boolean isMaybe, ResolvedType type) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        verifier.verifyType(type, loc);
        lhs.verifyGenericArgCounts(verifier);
    }

    //There's going to need to be special casing for this.
    //1. Special Case: Any numeric primitive can be cast into any numeric primitive, without fail.
    //1a. For this reason, the "as?" operator does not make sense if casting into a numeric type, so this is a type check error.
    //2. If the two types we're casting between have no supertyping relationship (ignoring the numeric case) this is a type check error.
    //2a. In the same vein, if the two types are the same, this cast is useless and so is labeled a type check error as well (since you almost certainly didn't mean to do it)
    //3. Regular case: A supertype can be cast into a subtype.
    //3a. If isMaybe is true, the output of "supertype as? subtype" is Option<subtype>.
    //3b. If isMaybe is false, the output of "supertype as subtype" is subtype, but it will panic at runtime if the type doesn't match.
    //4. A subtype can be cast into a supertype. This conversion cannot fail, and so "as?" does not work here, and the output type is the supertype.
    //   Performing this action does not usually have much purpose.

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics) throws CompilationException {
        //First get my type in the pool.
        TypeDef myTypeDef = checker.getOrInstantiate(type, typeGenerics);
        //Also infer the lhs:
        TypedExpr inferredLhs = lhs.infer(currentType, checker, typeGenerics);
        TypeDef lhsTypeDef = inferredLhs.type();

        //Case 1: numeric type -> numeric type:
        if (myTypeDef.builtin() instanceof IntegerType || myTypeDef.builtin() instanceof FloatType) {
            if (isMaybe)
                throw new TypeCheckingException("The \"as?\" operator cannot be used to convert to a numeric type; the conversion will always succeed! Use regular \"as\".", loc);
            if (lhsTypeDef.isNumeric()) {
                //Happy path: lhs is numeric; this cast will succeed!

                //Just do a quick check if lhs is a literal; if it is,
                //then perform the cast at compile time for constant folding.
                if (inferredLhs instanceof TypedLiteral literal)
                    return new TypedLiteral(loc, compileTimeCast(literal, (BuiltinTypeDef) myTypeDef.get()), myTypeDef);
                return new TypedCast(loc, tokenLine, inferredLhs, false, myTypeDef);
            }
            //Lhs cannot be numeric, or else its typedef would have been a builtin
            throw new TypeCheckingException("Only numeric types can be casted to numeric types like " + myTypeDef.name() + ", but the expression has type " + lhsTypeDef.name(), loc);
        }

        //Remaining cases: Check subtyping relationships
        if (lhsTypeDef.isSubtype(myTypeDef)) {
            if (myTypeDef.isSubtype(inferredLhs.type())) {
                //Case 2a: Subtypes of each other, so they must be the same type. Error.
                throw new TypeCheckingException("Cannot cast from type \"" + myTypeDef.name() + "\" to itself. This would be useless, and so is likely to be a bug", loc);
            } else {
                //Case 4: Casting a subtype to a supertype.
                //Similar to numeric types above, the "as?" operator is useless here, so error if we see it.
                if (isMaybe)
                    throw new TypeCheckingException("The \"as?\" operator cannot be used to convert from subtype to supertype; the conversion will always succeed! Use regular \"as\" instead here.", loc);
                //Otherwise, the cast is successful.
                return new TypedCast(loc, tokenLine, inferredLhs, false, myTypeDef);
            }
        } else {
            if (myTypeDef.isSubtype(inferredLhs.type())) {
                //Case 3: Casting a supertype to a subtype.
                //This is the interesting one.
                if (isMaybe) {
                    //Create the type Option<myType> and return it
                    TypeDef optionWrapped = checker.getGenericBuiltin(OptionType.INSTANCE, List.of(myTypeDef));
                    return new TypedCast(loc, tokenLine, inferredLhs, true, optionWrapped);
                } else {
                    return new TypedCast(loc, tokenLine, inferredLhs, false, myTypeDef);
                }
            } else {
                //Case 2: Neither is a subtype of the other. Error.
                throw new TypeCheckingException("Cannot cast from type \"" + lhsTypeDef.name() + "\" to \"" + myTypeDef.name() + "\", as they do not have a supertype-subtype relationship.", loc);
            }
        }
    }

    //Deal with compile time casting, for constant folding
    private Object compileTimeCast(TypedLiteral leftLit, BuiltinTypeDef myTypeDef) throws CompilationException {
        //This handles BigInteger, Fraction, Float, and Double
        if (leftLit.obj() instanceof Number number) {
            if (myTypeDef.builtin() instanceof IntegerType integerType) {
                //Num -> Int type
                BigInteger intValue = BigInteger.valueOf(switch (integerType.bits) {
                    case 8 -> number.byteValue();
                    case 16 -> number.shortValue();
                    case 32 -> number.intValue();
                    case 64 -> number.longValue();
                    default -> throw new IllegalStateException("Invalid bit count? Bug in compiler, please report!");
                });
                if (!integerType.signed)
                    intValue = IntegerType.toUnsigned(intValue, integerType.bits);
                return intValue;
            } else if (myTypeDef.builtin() instanceof FloatType floatType) {
                //Num -> Float type
                return switch (floatType.bits) {
                    case 32 -> number.floatValue();
                    case 64 -> number.doubleValue();
                    default -> throw new IllegalStateException("Invalid bit count? Bug in compiler, please report!");
                };
            }
        }
        throw new TypeCheckingException("Type " + leftLit.type().name() + " cannot be cast to " + myTypeDef.name(), loc);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected) throws CompilationException {
        TypedExpr inferred = infer(currentType, checker, typeGenerics);
        if (!inferred.type().isSubtype(expected))
            throw new TypeCheckingException("Expected " + expected.name() + ", but \"as\" expression resulted in " + inferred.type().name(), loc);
        return inferred;
    }
}
