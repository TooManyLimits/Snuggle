package ast.type_resolved.def.type;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.def.field.SnuggleTypeResolvedFieldDef;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.ClassDef;
import ast.typed.def.type.StructDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.ObjType;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.GenericStringUtil;
import util.ListUtils;

import java.util.List;

public record TypeResolvedStructDef(Loc loc, String name, int numGenerics, boolean nested, List<SnuggleTypeResolvedMethodDef> methods, List<SnuggleTypeResolvedFieldDef> fields) implements TypeResolvedTypeDef {

    @Override
    public void verifyGenericCounts(GenericVerifier verifier) throws CompilationException {
        //Verify each method and field
        for (SnuggleTypeResolvedMethodDef methodDef : methods)
            methodDef.verifyGenericCounts(verifier);
        for (SnuggleTypeResolvedFieldDef fieldDef : fields)
            fieldDef.verifyGenericCounts(verifier);
    }

    @Override
    public TypeDef instantiate(TypeDef currentType, TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef.InstantiationStackFrame newStackFrame = new TypeDef.InstantiationStackFrame(instantiationLoc, currentType, cause);
        List<MethodDef> typeInstantiatedMethods = ListUtils.map(methods, m -> m.instantiateType(methods, currentType, checker, generics, newStackFrame));
        List<FieldDef> typeInstantiatedFields = ListUtils.map(fields, f -> f.instantiateType(currentType, checker, generics, newStackFrame));
        return new StructDef(loc, GenericStringUtil.instantiateName(name, generics), typeInstantiatedFields, typeInstantiatedMethods);
    }
}
