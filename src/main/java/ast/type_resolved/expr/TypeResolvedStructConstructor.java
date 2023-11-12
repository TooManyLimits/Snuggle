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
        if (type != null)
            verifier.verifyType(type, loc);
        for (TypeResolvedExpr e : argValues)
            e.verifyGenericArgCounts(verifier);
    }

    private boolean named() {
        return argKeys != null;
    }

    private TypedExpr typeWithKnownType(TypeDef typeToConstruct, TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        if (!(typeToConstruct.get() instanceof StructDef))
            throw new TypeCheckingException("Struct constructors can only be used to create structs, but " + typeToConstruct.name() + " is not a struct!", loc, cause);

        List<TypedExpr> checkedValues = new ArrayList<>(argValues.size());
        if (named()) {
            int numFields = 0; //error tracker
            for (FieldDef f : typeToConstruct.nonStaticFields()) {
                numFields++;

                int index = argKeys.indexOf(f.name());
                if (index == -1)
                    throw new TypeCheckingException("Struct constructor for " + typeToConstruct.name() + " is missing field " + f.name(), loc, cause);

                TypedExpr checked = argValues.get(index).check(currentType, checker, typeGenerics, methodGenerics, f.type(), cause);
                checkedValues.add(checked);
            }
            if (numFields != argValues.size()) {
                Set<String> actualFieldNames = typeToConstruct.nonStaticFields().stream().map(FieldDef::name).collect(Collectors.toSet());
                Set<String> providedFieldNames = new HashSet<>(argKeys);
                providedFieldNames.removeAll(actualFieldNames);
                throw new TypeCheckingException("Struct constructor for " + typeToConstruct.name() + " has too many elements - expected only " + numFields + ", got " + argValues.size() + ". Fields " + providedFieldNames + " are not defined on this type.", loc, cause);
            }
        } else {
            int i = 0;
            for (FieldDef f : typeToConstruct.nonStaticFields()) {
                if (i >= argValues.size())
                    throw new TypeCheckingException("Struct constructor for " + typeToConstruct.name() + " does not have enough elements", loc, cause);
                TypedExpr checked = argValues.get(i).check(currentType, checker, typeGenerics, methodGenerics, f.type(), cause);
                checkedValues.add(checked);
                i++;
            }
            if (i != argValues.size())
                throw new TypeCheckingException("Struct constructor for " + typeToConstruct.name() + " has too many elements - expected only " + i + ", got " + argValues.size(), loc, cause);
        }
        return new TypedStructConstructor(loc, typeToConstruct, checkedValues);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        if (type == null)
            throw new TypeCheckingException("Unable to infer type of struct constructor: consider making it explicit or add annotations.", loc, cause);
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
            throw new TypeCheckingException(expected, "struct constructor", typed.type(), loc, cause);
        return typed;
    }
}
