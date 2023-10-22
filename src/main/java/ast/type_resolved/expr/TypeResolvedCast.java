package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedCast;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntLiteralType;
import builtin_types.types.numbers.IntegerType;
import exceptions.CompilationException;
import exceptions.TypeCheckingException;
import lexing.Loc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public record TypeResolvedCast(Loc loc, TypeResolvedExpr lhs, boolean isMaybe, ResolvedType type) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        verifier.verifyType(type, loc);
        lhs.verifyGenericArgCounts(verifier);
    }

    //There's going to need to be special casing for this.
    //1. Special Case: Any numeric type can be cast into any numeric type, without fail.
    //1a. For this reason, the "as?" operator does not make sense if casting into a numeric type, so this is a type check error.
    //2. If the type we're trying to cast to is one without any supertypes (and it isn't a numeric type), this is an error.
    //3. Regular case: A supertype can be cast into a subtype.
    //3a. If isMaybe is true, the output of "supertype as? subtype" is Option<subtype>.
    //3b. If isMaybe is false, the output of "supertype as subtype" is subtype, but it will panic at runtime if the type doesn't match.
    //4. A subtype can be cast into a supertype. This conversion cannot fail, and so "as?" does not work here, and the output type is the supertype.
    //   Performing this action does not generally have much purpose.

    @Override
    public TypedExpr infer(Type currentType, TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        //First get my type in the pool.
        Type myType = checker.pool().getOrInstantiateType(type, typeGenerics);
        TypeDef myTypeDef = checker.pool().getTypeDef(myType);
        //Also infer the lhs:
        TypedExpr inferredLhs = lhs.infer(currentType, checker, typeGenerics);
        TypeDef lhsTypeDef = checker.pool().getTypeDef(inferredLhs.type());

        //Case 1: numeric type -> numeric type:
        if (myTypeDef instanceof BuiltinTypeDef b && (b.builtin() instanceof IntegerType || b.builtin() instanceof FloatType)) {
            if (isMaybe)
                throw new TypeCheckingException("The \"as?\" operator cannot be used to convert to a numeric type; the conversion will always succeed! Use regular \"as\".", loc);
            if (lhsTypeDef instanceof BuiltinTypeDef b2) {
                if (!b2.isNumeric())
                    throw new TypeCheckingException("Only numeric types can be casted to numeric types like " + myTypeDef.name() + ", but the expression has type " + lhsTypeDef.name(), loc);
                //Happy path: lhs is numeric; this cast will succeed!

                //Just do a quick check if lhs is a literal; if it is,
                //then perform the cast at compile time for constant folding.
                if (inferredLhs instanceof TypedLiteral literal)
                    return new TypedLiteral(loc, compileTimeCast(literal, b), myType);
                return new TypedCast(loc, inferredLhs, false, myType);
            }
            //Lhs cannot be numeric, or else its typedef would have been a builtin
            throw new TypeCheckingException("Only numeric types can be casted to numeric types like " + myTypeDef.name() + ", but the expression has type " + lhsTypeDef.name(), loc);
        }

        return null;
    }

    //Deal with compile time casting, for constant folding
    private Object compileTimeCast(TypedLiteral leftLit, BuiltinTypeDef myTypeDef) throws CompilationException {
//        //If left is a BigInteger
//        if (leftLit.obj() instanceof BigInteger bigInteger) {
//            if (myTypeDef.builtin() instanceof IntegerType integerType) {
//                if (integerType.signed) {
//                    return BigInteger.valueOf(switch (integerType.bits) {
//                        case 8 -> bigInteger.byteValue();
//                        case 16 -> bigInteger.shortValue();
//                        case 32 -> bigInteger.intValue();
//                        case 64 -> bigInteger.longValue();
//                        default -> throw new IllegalStateException("Invalid bit count? Bug in compiler, please report!");
//                    });
//                } else {
//                    return bigInteger.and(BigInteger.ONE.shiftLeft(integerType.bits).subtract(BigInteger.ONE));
//                }
//            } else if (myTypeDef.builtin() instanceof FloatType floatType) {
//                return BigDecimal.valueOf(switch (floatType.bits) {
//                    case 32 -> bigInteger.floatValue();
//                    case 64 -> bigInteger.doubleValue();
//                    default -> throw new IllegalStateException("Invalid bit count? Bug in compiler, please report!");
//                });
//            } else {
//                throw new IllegalStateException("Type def was numeric, but not IntegerType or FloatType? Bug in compiler, please report!");
//            }
//        } else if (leftLit.obj() instanceof BigDecimal bigDecimal) {
//            if (myTypeDef.builtin() instanceof IntegerType integerType) {
//                if (integerType.signed) {
//                    return BigInteger.valueOf(switch (integerType.bits) {
//                        case 8 -> bigDecimal.byteValue();
//                        case 16 -> bigDecimal.shortValue();
//                        case 32 -> bigDecimal.intValue();
//                        case 64 -> bigDecimal.longValue();
//                        default -> throw new IllegalStateException("Invalid bit count? Bug in compiler, please report!");
//                    });
//                } else {
//                    return bigDecimal.longValue()
//                    return bigInteger.and(BigInteger.ONE.shiftLeft(integerType.bits).subtract(BigInteger.ONE));
//                }
//            } else if (myTypeDef.builtin() instanceof FloatType floatType) {
//                return BigDecimal.valueOf(switch (floatType.bits) {
//                    case 32 -> bigInteger.floatValue();
//                    case 64 -> bigInteger.doubleValue();
//                    default -> throw new IllegalStateException("Invalid bit count? Bug in compiler, please report!");
//                });
//            } else {
//                throw new IllegalStateException("Type def was numeric, but not IntegerType or FloatType? Bug in compiler, please report!");
//            }
//        } else {
//
//        }
        return null;


    }

    @Override
    public TypedExpr check(Type currentType, TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        return null;
    }
}
