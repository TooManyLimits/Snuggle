package ast.type_resolved.def.type;

import ast.type_resolved.def.field.SnuggleTypeResolvedFieldDef;
import ast.typed.def.field.SnuggleFieldDef;
import ast.typed.def.method.SnuggleMethodDef;
import builtin_types.types.ObjType;
import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import ast.typed.Type;
import ast.typed.def.type.ClassDef;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

public record TypeResolvedClassDef(Loc loc, String name, int numGenerics, ResolvedType.Basic supertype, List<SnuggleTypeResolvedMethodDef> methods, List<SnuggleTypeResolvedFieldDef> fields) implements TypeResolvedTypeDef {

    @Override
    public void verifyGenericCounts(GenericVerifier verifier) throws CompilationException {
        //Verify supertype
        if (supertype != null)
            verifier.verifyType(supertype, loc);

        //Verify each method and field
        for (SnuggleTypeResolvedMethodDef methodDef : methods)
            methodDef.verifyGenericCounts(verifier);
        for (SnuggleTypeResolvedFieldDef fieldDef : fields)
            fieldDef.verifyGenericCounts(verifier);
    }

    @Override
    public ClassDef instantiate(int index, TypeChecker checker, List<Type> generics) throws CompilationException {
        Type currentType = new Type.Basic(index);
        Type instantiatedSupertype = supertype == null ? checker.pool().getBasicBuiltin(ObjType.INSTANCE) : checker.pool().getOrInstantiateType(supertype, generics);
        List<SnuggleMethodDef> typeInstantiatedMethods = ListUtils.map(methods, m -> m.instantiateType(currentType, checker, generics));
        List<SnuggleFieldDef> typeInstantiatedFields = ListUtils.map(fields, f -> f.instantiateType(currentType, checker, generics));
        return new ClassDef(loc, index, instantiateName(checker, generics), instantiatedSupertype, typeInstantiatedMethods, typeInstantiatedFields);
    }

}
