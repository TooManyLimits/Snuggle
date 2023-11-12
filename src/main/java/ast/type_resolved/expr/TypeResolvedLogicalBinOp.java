package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.def.field.TypeResolvedFieldDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedBlock;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedLogicalBinOp;
import builtin_types.types.BoolType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedLogicalBinOp(Loc loc, boolean and, TypeResolvedExpr lhs, TypeResolvedExpr rhs) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        lhs.verifyGenericArgCounts(verifier);
        rhs.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypeDef bool = checker.getBasicBuiltin(BoolType.INSTANCE);
        //check both sides are bool
        TypedExpr typedLhs = lhs.check(currentType, checker, typeGenerics, methodGenerics, bool, cause);
        TypedExpr typedRhs = rhs.check(currentType, checker, typeGenerics, methodGenerics, bool, cause);
        //Const fold as much as we can
        if (typedLhs instanceof TypedLiteral lhsLiteral) {
            boolean lhs = ((Boolean) lhsLiteral.obj());
            if (typedRhs instanceof TypedLiteral rhsLiteral) {
                //Both are literals, condense into 1 literal
                boolean rhs = ((Boolean) rhsLiteral.obj());
                return new TypedLiteral(cause, loc, and ? lhs && rhs : lhs || rhs, bool);
            } else {
                //Lhs is a literal
                if (and != lhs) {
                    //true || __
                    //false && __
                    //If we can short circuit right away, then return the bool literal
                    return new TypedLiteral(cause, loc, lhs, bool);
                } else {
                    //false || __
                    //true && __
                    //Can't short circuit, just return rhs
                    return typedRhs;
                }
            }
        } else {
            if (typedRhs instanceof TypedLiteral rhsLiteral) {
                //Lhs is not literal, rhs is literal
                boolean rhs = ((Boolean) rhsLiteral.obj());
                if (and == rhs) {
                    //__ || false
                    //__ && true
                    //Don't care about rhs, just return the lhs
                    return typedLhs;
                } else {
                    //__ || true
                    //__ && false
                    //Eval the left side, then emit true or false
                    return new TypedBlock(loc, List.of(typedLhs, new TypedLiteral(cause, loc, rhs, bool)), bool);
                }
            } else {
                //Neither are literals, can't optimize more
                return new TypedLogicalBinOp(loc, and, typedLhs, typedRhs, bool);
            }
        }
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr result = infer(currentType, checker, typeGenerics, methodGenerics, cause);
        if (!result.type().isSubtype(expected))
            throw new TypeCheckingException(expected, (and ? "and" : "or") + "-expression", checker.getBasicBuiltin(BoolType.INSTANCE), loc, cause);
        return result;
    }
}
