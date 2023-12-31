package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedVariable;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import exceptions.compile_time.UndeclaredVariableException;
import lexing.Loc;

import java.util.List;


public record TypeResolvedVariable(Loc loc, String name, boolean isImplicitThis) implements TypeResolvedExpr {
    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        //do nothing
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypeDef t = checker.lookup(name);
        //If we're in a lambda, and either of:
        //- the variable name is "this"
        //- the variable does not exist
        //Then wrap it in a field access
        if (!isImplicitThis && checker.isLambda() && (t == null || name.equals("this"))) {
            return new TypeResolvedFieldAccess(loc, new TypeResolvedVariable(loc, "this", true), name, true).infer(currentType, checker, typeGenerics, methodGenerics, cause);
        } else if (t == null) {
            if (!name.equals("this") && checker.lookup("this") != null) {
                return new TypeResolvedFieldAccess(loc, new TypeResolvedVariable(loc, "this", true), name, true).infer(currentType, checker, typeGenerics, methodGenerics, cause);
            }
            throw new UndeclaredVariableException("Variable \"" + name + "\" was not declared in this scope", loc);
        } else {
            return new TypedVariable(loc, name, t);
        }
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr e = infer(currentType, checker, typeGenerics, methodGenerics, cause);
        if (!e.type().isSubtype(expected))
            throw new TypeCheckingException(expected, "local variable \"" + name + "\"", e.type(), loc, cause);
        return e;
    }
}
