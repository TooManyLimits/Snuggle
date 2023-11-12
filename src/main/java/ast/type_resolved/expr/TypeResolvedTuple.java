package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.def.type.TupleTypeDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedStructConstructor;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

public record TypeResolvedTuple(Loc loc, List<TypeResolvedExpr> elements) implements TypeResolvedExpr {
    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        for (TypeResolvedExpr e : elements)
            e.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        List<TypedExpr> typedExprs = ListUtils.map(elements, e -> e.infer(currentType, checker, typeGenerics, methodGenerics, cause));
        TypeDef type = checker.getTuple(ListUtils.map(typedExprs, TypedExpr::type));
        return new TypedStructConstructor(loc, type, typedExprs); //TypedTuple would have the exact same structure as a StructConstructor
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        if (expected.get() instanceof TupleTypeDef tupleDef) {
            //Check() each individual element
            if (tupleDef.elementTypes.size() != elements.size())
                throw new TypeCheckingException("Expected tuple with " + tupleDef.elementTypes.size() + " elements, but got one with " + elements.size() + " elements", loc, cause);

            List<TypedExpr> typedExprs = ListUtils.mapTwo(tupleDef.elementTypes, elements,
                    (expectedElement, element) -> element.check(currentType, checker, typeGenerics, methodGenerics, expectedElement, cause));
            TypeDef type = checker.getTuple(ListUtils.map(typedExprs, TypedExpr::type));
            return new TypedStructConstructor(loc, type, typedExprs); //TypedTuple would have the exact same structure as a StructConstructor
        } else {
            throw new TypeCheckingException("Expected type \"" + expected.name() + "\", but tuple expression returns a tuple", loc, cause);
        }
    }
}
