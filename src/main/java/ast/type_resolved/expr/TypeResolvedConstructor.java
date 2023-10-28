package ast.type_resolved.expr;

import ast.typed.def.type.TypeDef;
import builtin_types.types.UnitType;
import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.expr.TypedConstructor;
import ast.typed.expr.TypedExpr;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedConstructor(Loc loc, ResolvedType type, List<TypeResolvedExpr> args) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        verifier.verifyType(type, loc);
        for (TypeResolvedExpr arg : args)
            arg.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics) throws CompilationException {
        //Lookup the best method
        TypeDef t = checker.getOrInstantiate(type, typeGenerics);
        TypeChecker.BestMethodInfo best = checker.getBestMethod(loc, currentType, t, "new", args, List.of(), typeGenerics, false, false, checker.getBasicBuiltin(UnitType.INSTANCE));
        return new TypedConstructor(loc, t, best.methodDef(), best.typedArgs());
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected) throws CompilationException {
        TypedExpr inferred = infer(currentType, checker, typeGenerics);
        if (!inferred.type().isSubtype(expected))
            throw new TypeCheckingException("Expected type " + expected.name() + ", got " + inferred.type().name(), loc);
        return inferred;
    }
}
