package ast.type_resolved.def.type;

import builtin_types.types.ObjType;
import exceptions.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.method.SnuggleTypeResolvedMethodDef;
import ast.typed.Type;
import ast.typed.def.type.ClassDef;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

public record TypeResolvedClassDef(Loc loc, String name, int numGenerics, ResolvedType.Basic supertype, List<SnuggleTypeResolvedMethodDef> methods) implements TypeResolvedTypeDef {

    @Override
    public void verifyGenericCounts(GenericVerifier verifier) throws CompilationException {
        //Verify supertype
        if (supertype != null)
            verifier.verifyType(supertype, loc);

        //Verify each method
        for (SnuggleTypeResolvedMethodDef methodDef : methods)
            methodDef.verifyGenericCounts(verifier);
    }

    @Override
    public ClassDef instantiate(int index, TypeChecker checker, List<Type> generics) throws CompilationException {
        StringBuilder newName = new StringBuilder(name);
        if (generics.size() > 0) {
            newName.append("<");
            for (Type t : generics) {
                newName.append(checker.pool().getTypeDef(t).name());
                newName.append(", ");
            }
            newName.delete(newName.length() - 2, newName.length());
            newName.append(">");
        }
        return new ClassDef(
                loc, index, newName.toString(),
                supertype == null ? checker.pool().getBasicBuiltin(ObjType.INSTANCE) : checker.pool().getOrInstantiateType(supertype, generics),
                ListUtils.map(methods, m -> m.instantiateType(index, checker, generics))
        );
    }

}
