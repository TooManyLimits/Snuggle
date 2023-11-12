package ast.type_resolved.def.type;

import ast.type_resolved.def.field.SnuggleTypeResolvedFieldDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import ast.typed.def.type.ClassDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.ObjType;
import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.GenericStringUtil;
import util.LateInit;
import util.ListUtils;

import java.util.List;

public record TypeResolvedClassDef(Loc loc, String name, int numGenerics, boolean nested, ResolvedType.Basic supertype, List<SnuggleTypeResolvedMethodDef> methods, List<SnuggleTypeResolvedFieldDef> fields) implements TypeResolvedTypeDef {

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

    //InstantiationStackFrame: Where was *this ClassDef* instantiated from?
    @Override
    public ClassDef instantiate(TypeDef currentType, TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef.InstantiationStackFrame newStackFrame = new TypeDef.InstantiationStackFrame(instantiationLoc, currentType, cause);
        LateInit<TypeDef, CompilationException> instantiatedSupertype = new LateInit<>(() -> {
            TypeDef res = supertype == null ? checker.getBasicBuiltin(ObjType.INSTANCE) : checker.getOrInstantiate(supertype, generics, List.of(), loc, newStackFrame);
            if (res != null && !res.extensible())
                throw new TypeCheckingException("Cannot extend from \"" + res.name() + "\"", loc, newStackFrame);
            return res;
        });
        List<MethodDef> typeInstantiatedMethods = ListUtils.map(methods, m -> m.instantiateType(methods, currentType, checker, generics, newStackFrame));
        List<FieldDef> typeInstantiatedFields = ListUtils.map(fields, f -> f.instantiateType(currentType, checker, generics, newStackFrame));
        return new ClassDef(loc, GenericStringUtil.instantiateName(name, generics), instantiatedSupertype, typeInstantiatedFields, typeInstantiatedMethods);
    }

}
