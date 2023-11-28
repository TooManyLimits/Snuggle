package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedDeclaration;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.ParsingException;
import lexing.Loc;

import java.util.List;

public record TypeResolvedDeclaration(Loc loc, String name, ResolvedType annotatedType, TypeResolvedExpr rhs) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        if (annotatedType != null)
            verifier.verifyType(annotatedType, loc);
        rhs.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedDeclaration infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        if (annotatedType == null) {
            //Infer rhs
            TypedExpr typedRhs = rhs.infer(currentType, checker, typeGenerics, methodGenerics, cause);
            //Ensure rhs isn't an un-storable value (e.g. a literal).
            //This following method errors if a storable type can't be found.
            TypeDef rhsType = typedRhs.type().compileTimeToRuntimeConvert(typedRhs.type(), typedRhs.loc(), cause, checker);
            //If it's a literal, can pull the type upwards on the spot
            if (typedRhs instanceof TypedLiteral literal)
                typedRhs = literal.pullTypeUpwards(rhsType);
            checker.declare(loc, name, rhsType);
            return new TypedDeclaration(loc, name, rhsType, typedRhs);
        } else {
            TypeDef instantiatedAnnotatedType = checker.getOrInstantiate(annotatedType, typeGenerics, methodGenerics, loc, cause);
            //Check rhs
            TypedExpr typedRhs = rhs.check(currentType, checker, typeGenerics, methodGenerics, instantiatedAnnotatedType, cause);
            checker.declare(loc, name, instantiatedAnnotatedType);
            return new TypedDeclaration(loc, name, instantiatedAnnotatedType, typedRhs);
        }
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //If we ever need to check() a declaration, it's in an invalid place, hence why we throw a parsing exception.
        throw new ParsingException("Invalid declaration location for variable \"" + name + "\"", loc);
//        throw new IllegalStateException("Error: Attempt to check() a declaration. This should be impossible - Bug in compiler, please report!");
    }
}
