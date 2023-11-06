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

    private TypedExpr typeWithKnownType(TypeDef typeToConstruct, TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics) throws CompilationException {
        if (!(typeToConstruct.get() instanceof StructDef))
            throw new TypeCheckingException("Struct constructors can only be used to create structs, but " + typeToConstruct.name() + " is not a struct!", loc);

        List<TypedExpr> checkedValues = new ArrayList<>(argValues.size());
        if (named()) {
            int numFields = 0; //error tracker
            for (FieldDef f : typeToConstruct.fields()) {
                if (f.isStatic()) continue;
                numFields++;

                int index = argKeys.indexOf(f.name());
                if (index == -1)
                    throw new TypeCheckingException("Struct constructor for " + typeToConstruct.name() + " is missing field " + f.name(), loc);

                TypedExpr checked = argValues.get(index).check(currentType, checker, typeGenerics, f.type());
                checkedValues.add(checked);
            }
            if (numFields != argValues.size()) {
                Set<String> actualFieldNames = typeToConstruct.fields().stream().map(FieldDef::name).collect(Collectors.toSet());
                Set<String> providedFieldNames = new HashSet<>(argKeys);
                providedFieldNames.removeAll(actualFieldNames);
                throw new TypeCheckingException("Struct constructor for " + typeToConstruct.name() + " has too many values - expected only " + numFields + ", got " + argValues.size() + ". Fields " + providedFieldNames + " are not defined on this type.", loc);
            }
        } else {
            int i = 0;
            for (FieldDef f : typeToConstruct.fields()) {
                if (f.isStatic()) continue;
                if (i >= argValues.size())
                    throw new TypeCheckingException("Struct constructor for " + typeToConstruct.name() + " does not have enough values", loc);
                TypedExpr checked = argValues.get(i).check(currentType, checker, typeGenerics, f.type());
                checkedValues.add(checked);
                i++;
            }
            if (i != argValues.size())
                throw new TypeCheckingException("Struct constructor for " + typeToConstruct.name() + " has too many values - expected only " + i + ", got " + argValues.size(), loc);
        }
        return new TypedStructConstructor(loc, typeToConstruct, checkedValues);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics) throws CompilationException {
        if (type == null)
            throw new TypeCheckingException("Unable to infer type of struct constructor: consider making it explicit or add annotations.", loc);
        TypeDef t = checker.getOrInstantiate(type, typeGenerics);
        return typeWithKnownType(t, currentType, checker, typeGenerics);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, TypeDef expected) throws CompilationException {
        //If type is explicit, use it
        //If not explicit, use expected type
        TypedExpr typed = typeWithKnownType(
                type == null ? expected : checker.getOrInstantiate(type, typeGenerics),
                currentType, checker, typeGenerics
        );
        if (!typed.type().isSubtype(expected))
            throw new TypeCheckingException("Expected type " + expected.name() + ", got " + typed.type().name(), loc);
        return typed;
    }
}
