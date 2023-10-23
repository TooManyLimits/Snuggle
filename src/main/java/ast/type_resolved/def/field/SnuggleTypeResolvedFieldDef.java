package ast.type_resolved.def.field;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.typed.Type;
import ast.typed.def.field.SnuggleFieldDef;
import ast.typed.expr.TypedExpr;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.LateInit;

import java.util.List;

//Initializer may be null
public record SnuggleTypeResolvedFieldDef(Loc loc, boolean pub, boolean isStatic, String name, ResolvedType annotatedType, TypeResolvedExpr initializer) implements TypeResolvedFieldDef {

    public void verifyGenericCounts(GenericVerifier verifier) throws CompilationException {
        verifier.verifyType(annotatedType, loc);
        if (initializer != null) initializer.verifyGenericArgCounts(verifier);
    }

    @Override
    public SnuggleFieldDef instantiateType(Type currentType, TypeChecker checker, List<Type> generics) throws CompilationException {
        Type initializedType = checker.pool().getOrInstantiateType(annotatedType, generics);
        return new SnuggleFieldDef(loc, pub, isStatic, name,
                initializedType,
                initializer == null ? null : new LateInit<>(() -> {
                    checker.push();
                    checker.declare(loc, "this", currentType);
                    TypedExpr res = initializer.check(currentType, checker, generics, initializedType);
                    checker.pop();
                    return res;
                })
        );
    }
}
