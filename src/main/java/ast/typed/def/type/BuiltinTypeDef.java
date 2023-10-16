package ast.typed.def.type;

import exceptions.CompilationException;
import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import lexing.Loc;
import util.LateInit;

import java.util.List;
import java.util.Set;

public record BuiltinTypeDef(BuiltinType builtin, List<Type> generics, int index, LateInit<List<? extends MethodDef>, CompilationException> localizedMethods, LateInit<Set<Type>, CompilationException> localizedSupertypes, LateInit<Type, CompilationException> localizedTrueSupertype) implements TypeDef {
    @Override
    public String name() {
        return builtin.name();
    }

    @Override
    public boolean isSubtype(Type other, TypePool pool) {
        try {
            return localizedSupertypes.get().contains(other);
        } catch (CompilationException e) {
            throw new IllegalStateException("Bug in setup for BuiltinType " + builtin.name() + ", failed to locate supertypes?");
        }
    }

    @Override
    public Type trueSupertype() throws CompilationException {
        return localizedTrueSupertype.get();
    }

    @Override
    public Type toStorable(Type thisType, Loc loc, TypePool pool) throws CompilationException {
        return builtin.toStorable(thisType, loc, pool);
    }

    @Override
    public void checkMethodBodies() throws CompilationException {
        
    }

    @Override
    public List<? extends MethodDef> getMethods() throws CompilationException {
        return localizedMethods.get();
    }

    @Override
    public String getDescriptor() {
        return builtin.getDescriptor(index);
    }

    @Override
    public String getGeneratedName() {
        throw new UnsupportedOperationException("Not implemented...");
    }
}
