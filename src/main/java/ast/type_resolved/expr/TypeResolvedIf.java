package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.TypeCheckingHelper;
import ast.typed.Type;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedIf;
import ast.typed.expr.TypedLiteral;
import builtin_types.types.BoolType;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedIf(Loc loc, TypeResolvedExpr cond, TypeResolvedExpr ifTrue, TypeResolvedExpr ifFalse) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        cond.verifyGenericArgCounts(verifier);
        ifTrue.verifyGenericArgCounts(verifier);
        if (hasFalseBranch())
            ifFalse.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(Type currentType, TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        TypedExpr typedCond = cond.check(currentType, checker, typeGenerics, checker.pool().getBasicBuiltin(BoolType.INSTANCE));
        //If the expr doesn't have a false branch, then the output type of the expression is Option<output of ifTrue>
        //If the expr does have a false branch, then the output types of the branches must match, and the output type
        //of the if-expression is that type.

        //If the condition is a constant bool, we can do special handling and choose the branch at compile time
        if (typedCond instanceof TypedLiteral literal && literal.obj() instanceof Boolean b) {
            if (b) {
                //Infer the true branch. If there's an else branch, return it as-is.
                TypedExpr inferredTrueBranch = ifTrue.infer(currentType, checker, typeGenerics);
                if (hasFalseBranch()) return inferredTrueBranch;
                //If there's no false branch, need to wrap this value in an Option<>.
                return TypeCheckingHelper.wrapInOption(loc, inferredTrueBranch, checker);
            } else {
                //If this has a false branch, return its inferred value:
                if (hasFalseBranch())
                    return ifFalse.infer(currentType, checker, typeGenerics);
                //Otherwise, if there's no false branch, need to return an empty Option<>.
                //Need to know the generic for the option, so infer the ifTrue branch and grab its type.
                Type trueType = ifTrue.infer(currentType, checker, typeGenerics).type();
                return TypeCheckingHelper.getEmptyOption(loc, trueType, checker);
            }
        }

        //Otherwise, need to output a TypedIf
        TypedExpr typedTrueBranch = ifTrue.infer(currentType, checker, typeGenerics);
        if (hasFalseBranch()) {
            TypedExpr typedFalseBranch = ifFalse.check(currentType, checker, typeGenerics, typedTrueBranch.type());
            return new TypedIf(loc, typedCond, typedTrueBranch, typedFalseBranch, typedTrueBranch.type());
        } else {
            throw new IllegalStateException("If expressions without else branches are not yet supported!");
        }
    }

    private boolean hasFalseBranch() {
        return ifFalse != null;
    }

    //Works similarly to infer(), but with some infers replaced with checks.
    @Override
    public TypedExpr check(Type currentType, TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        //Check condition is bool
        TypedExpr typedCond = cond.check(currentType, checker, typeGenerics, checker.pool().getBasicBuiltin(BoolType.INSTANCE));
        //Check if constant
        if (typedCond instanceof TypedLiteral literal && literal.obj() instanceof Boolean b) {
            if (b) {
                if (hasFalseBranch())
                    return ifTrue.check(currentType, checker, typeGenerics, expected);
                else {
                    throw new IllegalStateException("If expressions without else branches are not yet supported!");
                }
            } else {
                if (hasFalseBranch())
                    return ifFalse.check(currentType, checker, typeGenerics, expected);
                else {
                    throw new IllegalStateException("If expressions without else branches are not yet supported!");
                }
            }
        }
        //If not constant, need to check both branches
        if (hasFalseBranch()) {
            TypedExpr typedTrueBranch = ifTrue.check(currentType, checker, typeGenerics, expected);
            TypedExpr typedFalseBranch = ifFalse.check(currentType, checker, typeGenerics, expected);
            return new TypedIf(loc, typedCond, typedTrueBranch, typedFalseBranch, expected);
        } else {
            throw new IllegalStateException("If expressions without else branches are not yet supported!");
        }
    }
}
