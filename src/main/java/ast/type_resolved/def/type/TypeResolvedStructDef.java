package ast.type_resolved.def.type;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.def.field.SnuggleTypeResolvedFieldDef;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

public record TypeResolvedStructDef(Loc loc, String name, int numGenerics, List<SnuggleTypeResolvedMethodDef> methods, List<SnuggleTypeResolvedFieldDef> fields) { //implements TypeResolvedTypeDef {

//    @Override
//    public void verifyGenericCounts(GenericVerifier verifier) throws CompilationException {
//        //Verify each method and field
//        for (SnuggleTypeResolvedMethodDef methodDef : methods)
//            methodDef.verifyGenericCounts(verifier);
//        for (SnuggleTypeResolvedFieldDef fieldDef : fields)
//            fieldDef.verifyGenericCounts(verifier);
//    }
//
//    @Override
//    public TypeDef instantiate(int index, TypeChecker checker, List<Type> generics) throws CompilationException {
//        Type currentType = new Type.Basic(index);
//        List<SnuggleMethodDef> typeInstantiatedMethods = ListUtils.map(methods, m -> m.instantiateType(currentType, checker, generics));
//        List<SnuggleFieldDef> typeInstantiatedFields = ListUtils.map(fields, f -> f.instantiateType(currentType, checker, generics));
//        return new StructDef(loc, index, instantiateName(checker, generics), typeInstantiatedMethods, typeInstantiatedFields);
//    }
}
