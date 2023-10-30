package ast.type_resolved.expr;

import ast.parsed.ParsedType;
import ast.parsed.expr.ParsedExpr;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.StructDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedStructConstructor;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.ListUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record TypeResolvedStructConstructor(Loc loc, ResolvedType type, List<String> argKeys, List<TypeResolvedExpr> argValues) implements TypeResolvedExpr {

    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        verifier.verifyType(type, loc);
        for (TypeResolvedExpr e : argValues)
            e.verifyGenericArgCounts(verifier);
    }

    private boolean named() {
        return argKeys != null;
    }


    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics) throws CompilationException {
        TypeDef t = checker.getOrInstantiate(type, typeGenerics);

        if (!(t.get() instanceof StructDef))
            throw new TypeCheckingException("Struct constructors can only be used to create structs, but " + t.name() + " is not a struct!", loc);

        List<TypedExpr> checkedValues = new ArrayList<>(argValues.size());
        if (named()) {
            int numFields = 0; //error tracker
            for (FieldDef f : t.fields()) {
                if (f.isStatic()) continue;
                numFields++;

                int index = argKeys.indexOf(f.name());
                if (index == -1)
                    throw new TypeCheckingException("Struct constructor for " + t.name() + " is missing field " + f.name(), loc);

                TypedExpr checked = argValues.get(index).check(currentType, checker, typeGenerics, f.type());
                checkedValues.add(checked);
            }
            if (numFields != argValues.size()) {
                Set<String> actualFieldNames = t.fields().stream().map(FieldDef::name).collect(Collectors.toSet());
                Set<String> providedFieldNames = new HashSet<>(argKeys);
                providedFieldNames.removeAll(actualFieldNames);
                throw new TypeCheckingException("Struct constructor for " + t.name() + " has too many values - expected only " + numFields + ", got " + argValues.size() + ". Fields " + providedFieldNames + " are not defined on this type.", loc);
            }
        } else {
            int i = 0;
            for (FieldDef f : t.fields()) {
                if (f.isStatic()) continue;
                if (i >= argValues.size())
                    throw new TypeCheckingException("Struct constructor for " + t.name() + " does not have enough values", loc);
                TypedExpr checked = argValues.get(i).check(currentType, checker, typeGenerics, f.type());
                checkedValues.add(checked);
                i++;
            }
            if (i != argValues.size())
                throw new TypeCheckingException("Struct constructor for " + t.name() + " has too many values - expected only " + i + ", got " + argValues.size(), loc);
        }
        return new TypedStructConstructor(loc, t, checkedValues);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected) throws CompilationException {
        TypedExpr inferred = infer(currentType, checker, typeGenerics);
        if (!inferred.type().isSubtype(expected))
            throw new TypeCheckingException("Expected type " + expected.name() + ", got " + inferred.type().name(), loc);
        return inferred;
    }
}
