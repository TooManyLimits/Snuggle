package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.Type;
import ast.typed.expr.TypedDeclaration;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import exceptions.CompilationException;
import exceptions.ParsingException;
import exceptions.TypeCheckingException;
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
    public TypedDeclaration infer(TypeChecker checker, List<Type> typeGenerics) throws CompilationException {
        if (annotatedType == null) {
            //Infer rhs
            TypedExpr typedRhs = rhs.infer(checker, typeGenerics);
            //Ensure rhs isn't an un-storable value (e.g. a literal).
            //This following method errors if a storable type can't be found.
            Type rhsType = checker.pool().getTypeDef(typedRhs.type()).toStorable(typedRhs.type(), typedRhs.loc(), checker.pool());
            //If it's a literal, can pull the type upwards on the spot
            if (typedRhs instanceof TypedLiteral literal)
                typedRhs = literal.pullTypeUpwards(rhsType);
            checker.declare(loc, name, rhsType);
            return new TypedDeclaration(loc, name, rhsType, typedRhs);
        } else {
            Type instantiatedAnnotatedType = checker.pool().getOrInstantiateType(annotatedType, typeGenerics);
            //Check rhs
            TypedExpr typedRhs = rhs.check(checker, typeGenerics, instantiatedAnnotatedType);
            checker.declare(loc, name, instantiatedAnnotatedType);
            return new TypedDeclaration(loc, name, instantiatedAnnotatedType, typedRhs);
        }
    }

    @Override
    public TypedExpr check(TypeChecker checker, List<Type> typeGenerics, Type expected) throws CompilationException {
        throw new ParsingException("Invalid declaration location for variable \"" + name + "\"", loc);
//        throw new IllegalStateException("Error: Attempt to check() a declaration. This should be impossible - Bug in compiler, please report!");
    }
}
