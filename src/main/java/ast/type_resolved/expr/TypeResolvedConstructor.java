package ast.type_resolved.expr;

import ast.typed.def.type.StructDef;
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
        if (type != null)
            verifier.verifyType(type, loc);
        for (TypeResolvedExpr arg : args)
            arg.verifyGenericArgCounts(verifier);
    }

    private TypedExpr typeWithKnownType(TypeDef typeToConstruct, TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Get the expected return type of the new() method
        TypeDef expectedConstructorOutput = typeToConstruct.get() instanceof StructDef sd ? sd : checker.getBasicBuiltin(UnitType.INSTANCE);
        //Lookup the best method
        TypeChecker.BestMethodInfo best = checker.getBestMethod(loc, currentType, typeToConstruct, "new", args, List.of(), typeGenerics, methodGenerics, false, false, expectedConstructorOutput, cause);
        return new TypedConstructor(loc, typeToConstruct, best.methodDef(), best.typedArgs());
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //If null, can't infer.
        if (type == null)
            throw new TypeCheckingException("Unable to infer type of constructor: consider making it explicit or add annotations.", loc, cause);
        TypeDef t = checker.getOrInstantiate(type, typeGenerics, methodGenerics, loc, cause);
        return typeWithKnownType(t, currentType, checker, typeGenerics, methodGenerics, cause);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //If type is explicit, use it
        //If not explicit, use expected type
        TypedExpr typed = typeWithKnownType(
                type == null ? expected : checker.getOrInstantiate(type, typeGenerics, methodGenerics, loc, cause),
                currentType, checker, typeGenerics, methodGenerics, cause
        );
        if (!typed.type().isSubtype(expected))
            throw new TypeCheckingException(expected, "constructor", typed.type(), loc, cause);
        return typed;
    }
}
