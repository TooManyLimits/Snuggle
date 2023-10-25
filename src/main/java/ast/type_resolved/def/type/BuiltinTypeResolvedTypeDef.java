package ast.type_resolved.def.type;

import exceptions.compile_time.CompilationException;
import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import util.LateInit;

import java.util.List;

public record BuiltinTypeResolvedTypeDef(BuiltinType builtin) implements TypeResolvedTypeDef {

    @Override
    public String name() {
        return builtin.name();
    }

    @Override
    public int numGenerics() {
        return builtin.numGenerics();
    }

    @Override
    public void verifyGenericCounts(GenericVerifier verifier) throws CompilationException {

    }

    @Override
    public TypeDef instantiate(int index, TypeChecker checker, List<Type> generics) throws CompilationException {
        //TODO: Verify with the BuiltinType that this is okay
        return new BuiltinTypeDef(builtin,
                builtin.genericName(generics, checker.pool()),
                builtin.getDescriptor(generics, checker.pool()),
                builtin.getRuntimeName(generics, checker.pool()),
                builtin.isReferenceType(generics, checker.pool()),
                builtin.hasSpecialConstructor(generics, checker.pool()),
                generics,
                index,
                new LateInit<>(() -> builtin.getMethods(generics, checker.pool())),
                new LateInit<>(() -> builtin.getSupertypes(generics, checker.pool())),
                new LateInit<>(() -> builtin.getTrueSupertype(generics, checker.pool()))
        );
    }

}
