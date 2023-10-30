package ast.type_resolved.def.field;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.typed.def.field.FieldDef;
import ast.typed.def.field.SnuggleFieldDef;
import ast.typed.def.type.TypeDef;
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
    public SnuggleFieldDef instantiateType(TypeDef currentType, TypeChecker checker, List<TypeDef> generics) {
        TypeDef initializedType = checker.getOrInstantiate(annotatedType, generics);
        return new SnuggleFieldDef(loc, pub, name, currentType, initializedType, isStatic);
//                initializer == null ? null : new LateInit<>(() -> {
//                    checker.push();
//                    checker.declare(loc, "this", currentType);
//                    TypedExpr res = initializer.check(currentType, checker, generics, initializedType);
//                    checker.pop();
//                    return res;
//                })
//        );
    }
}
