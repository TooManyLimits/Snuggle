package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.def.field.TypeResolvedFieldDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.field.SnuggleFieldDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedFieldAccess;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import exceptions.compile_time.UndeclaredVariableException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

//If this field access was created with an implicit this, it changes its error message.
public record TypeResolvedFieldAccess(Loc loc, TypeResolvedExpr lhs, String name, boolean isImplicitThis) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        lhs.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr typedLhs = lhs.infer(currentType, checker, typeGenerics, cause);
        FieldDef field = ListUtils.find(typedLhs.type().nonStaticFields(), f -> {
            if (!f.name().equals(name)) return false;
            if (f.pub()) return true;
            if (f instanceof SnuggleFieldDef sf)
                return sf.loc().fileName().equals(loc.fileName());
            throw new IllegalStateException("Non-snuggle field defs should always be pub? Bug in compiler, please report");
        });
        if (field == null) {
            if (isImplicitThis)
                throw new UndeclaredVariableException("Unable to find variable \"" + name + "\" as either local variable or field of current class", loc);
            else
                throw new UndeclaredVariableException("Unable to locate field \"" + name + "\" on type " + typedLhs.type().name(), loc);
        }
        return new TypedFieldAccess(loc, typedLhs, field, field.type());
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypedExpr e = infer(currentType, checker, typeGenerics, cause);
        if (!e.type().isSubtype(expected))
            throw new TypeCheckingException(expected, "field '" + name + "'", e.type(), loc, cause);
        return e;
    }
}
