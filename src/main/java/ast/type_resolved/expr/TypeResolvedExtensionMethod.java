package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedStructConstructor;
import builtin_types.types.ExtensionMethods;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.Mutable;

import java.util.List;

public record TypeResolvedExtensionMethod(Loc loc, SnuggleTypeResolvedMethodDef typeResolvedMethodDef, boolean isTopLevel, Mutable<SnuggleMethodDef> methodDef) implements TypeResolvedExpr {

    public TypeResolvedExtensionMethod(Loc loc, SnuggleTypeResolvedMethodDef typeResolvedMethodDef, boolean isTopLevel) {
        this(loc, typeResolvedMethodDef, isTopLevel, new Mutable<>());
    }

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        typeResolvedMethodDef.verifyGenericCounts(verifier);
    }

    public void addToCheckerEnvironment(TypeChecker checker, List<TypeDef> typeGenerics, TypeDef.InstantiationStackFrame cause, boolean importingForTopLevel) throws CompilationException {
        if (methodDef.v == null) {
            //Get the type that is to contain all the extension methods
            TypeDef extensionMethodsType = checker.getBasicBuiltin(ExtensionMethods.INSTANCE);
            //Calculate disambiguation index. Almost identical code as in SnuggleTypeResolvedMethodDef
            int disambiguationIndex = 0;
            for (MethodDef method : extensionMethodsType.methods())
                if (method.name().equals(typeResolvedMethodDef.name()))
                    disambiguationIndex++;
            //Instantiate the method def (with the *type* generics)
            SnuggleMethodDef typeInstantiated = typeResolvedMethodDef.instantiateType(disambiguationIndex, extensionMethodsType, checker, typeGenerics, cause);
            methodDef.v = typeInstantiated;
            //And add it to the ExtensionMethods type.
            extensionMethodsType.addMethod(typeInstantiated);
        }
        if (importingForTopLevel == isTopLevel)
            checker.addExtensionMethod(methodDef.v);
        methodDef.v.checkCode();
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        addToCheckerEnvironment(checker, typeGenerics, cause, false);
        //And return a simple unit as the type
        return new TypedStructConstructor(loc, checker.getTuple(List.of()), List.of());
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //No special process for checking
        TypedExpr inferred = infer(currentType, checker, typeGenerics, methodGenerics, cause);
        if (!inferred.type().isSubtype(expected))
            throw new TypeCheckingException(expected, "Extension Method definition", inferred.type(), loc, cause);
        return inferred;
    }
}
