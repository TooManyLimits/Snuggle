package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.TypeCheckingHelper;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedConstructor;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedIf;
import ast.typed.expr.TypedLiteral;
import builtin_types.types.BoolType;
import builtin_types.types.OptionType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
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
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr typedCond = cond.check(currentType, checker, typeGenerics, methodGenerics, checker.getBasicBuiltin(BoolType.INSTANCE), cause);
        //If the expr doesn't have a false branch, then the output type of the expression is Option<output of ifTrue>
        //If the expr does have a false branch, then the output topLevelTypes of the branches must match, and the output type
        //of the if-expression is that type.

        //If the condition is a constant bool, we can do special handling and choose the branch at compile time
        if (typedCond instanceof TypedLiteral literal && literal.obj() instanceof Boolean b) {
            if (b) {
                //Infer the true branch. If there's an else branch, return it as-is.
                TypedExpr inferredTrueBranch = ifTrue.infer(currentType, checker, typeGenerics, methodGenerics, cause);
                if (hasFalseBranch()) return inferredTrueBranch;
                //If there's no false branch, need to wrap this value in an Option<>.
                return TypeCheckingHelper.wrapInOption(loc, inferredTrueBranch, checker, cause);
            } else {
                //If this has a false branch, return its inferred value:
                if (hasFalseBranch())
                    return ifFalse.infer(currentType, checker, typeGenerics, methodGenerics, cause);
                //Otherwise, if there's no false branch, need to return an empty Option<>.
                //Need to know the generic for the option, so infer the ifTrue branch and grab its type.
                TypeDef trueType = ifTrue.infer(currentType, checker, typeGenerics, methodGenerics, cause).type();
                return TypeCheckingHelper.getEmptyOption(loc, trueType, checker, cause);
            }
        }

        //Otherwise, need to output a TypedIf
        TypedExpr typedTrueBranch = ifTrue.infer(currentType, checker, typeGenerics, methodGenerics, cause);
        if (hasFalseBranch()) {
            TypedExpr typedFalseBranch = ifFalse.check(currentType, checker, typeGenerics, methodGenerics, typedTrueBranch.type(), cause);
            return new TypedIf(loc, typedCond, typedTrueBranch, typedFalseBranch, typedTrueBranch.type());
        } else {
            TypedExpr wrappedTrueBranch = TypeCheckingHelper.wrapInOption(loc, typedTrueBranch, checker, cause);
            TypedExpr generatedFalseBranch = TypeCheckingHelper.getEmptyOption(loc, typedTrueBranch.type(), checker, cause);
            return new TypedIf(loc, typedCond, wrappedTrueBranch, generatedFalseBranch, wrappedTrueBranch.type());
        }
    }

    private boolean hasFalseBranch() {
        return ifFalse != null;
    }

    //Works similarly to infer(), but with some infers replaced with checks.
    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Check condition is bool
        TypedExpr typedCond = cond.check(currentType, checker, typeGenerics, methodGenerics, checker.getBasicBuiltin(BoolType.INSTANCE), cause);
        //Check if constant
        if (typedCond instanceof TypedLiteral literal && literal.obj() instanceof Boolean b) {

            //If there's an else branch, check the chosen branch is as expected, and return it.
            if (hasFalseBranch())
                return (b ? ifTrue : ifFalse).check(currentType, checker, typeGenerics, methodGenerics, expected, cause);

            //Otherwise, we need to make sure that the expected type is, in fact, an Option.
            if (!(expected.builtin() == OptionType.INSTANCE))
                throw new TypeCheckingException("Expected type \"" + expected.name() + "\", but if-expression without else-branch results in Option", loc, cause);

            //Once we know it is, get the inner type of the option.
            TypeDef innerType = ((BuiltinTypeDef) expected.get()).generics.get(0);

            TypedConstructor constructor; //output
            if (b) {
                //If there's no else branch, ensure that the true branch is the inner type of the option.
                TypedExpr checkedTrueBranch = ifTrue.check(currentType, checker, typeGenerics, methodGenerics, innerType, cause);
                //And then wrap the result in an option.
                constructor = TypeCheckingHelper.wrapInOption(loc, checkedTrueBranch, checker, cause);
            } else {
                //If there's no else branch, return an empty Option of the desired type.
                //Don't bother checking the ifTrue branch.
                constructor = TypeCheckingHelper.getEmptyOption(loc, innerType, checker, cause);
            }

            //Finally, ensure this option is what was wanted.
            if (!constructor.type().isSubtype(expected))
                throw new TypeCheckingException(expected, "if-expression", constructor.type(), loc, cause);
            return constructor;
        }

        //If not constant, need to check both branches
        if (hasFalseBranch()) {
            TypedExpr typedTrueBranch = ifTrue.check(currentType, checker, typeGenerics, methodGenerics, expected, cause);
            TypedExpr typedFalseBranch = ifFalse.check(currentType, checker, typeGenerics, methodGenerics, expected, cause);
            return new TypedIf(loc, typedCond, typedTrueBranch, typedFalseBranch, expected);
        } else {
            //If there's only one branch, only check it for the inner type.
            //Make sure that the expected type is an Option:
            if (!(expected.builtin() == OptionType.INSTANCE))
                throw new TypeCheckingException("Expected type \"" + expected.name() + "\", but if-expression without else-branch results in Option", loc, cause);
            //Once we know it is, get the inner type of the option
            TypeDef innerType = ((BuiltinTypeDef) expected.get()).generics.get(0);
            //Check the true branch for the inner type of the option
            TypedExpr typedTrueBranch = ifTrue.check(currentType, checker, typeGenerics, methodGenerics, innerType, cause);
            //Wrap the true branch in an option constructor
            TypedConstructor wrappedTrueBranch = TypeCheckingHelper.wrapInOption(loc, typedTrueBranch, checker, cause);
            //And generate an else branch, which is just an empty option constructor
            TypedConstructor generatedFalseBranch = TypeCheckingHelper.getEmptyOption(loc, innerType, checker, cause);
            //And return a TypedIf for this situation
            return new TypedIf(loc, typedCond, wrappedTrueBranch, generatedFalseBranch, expected);
        }
    }
}
