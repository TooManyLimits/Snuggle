package ast.typed.def.type;

import ast.typed.def.field.FieldDef;
import builtin_types.types.numbers.FloatLiteralType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntLiteralType;
import builtin_types.types.numbers.IntegerType;
import exceptions.compile_time.CompilationException;
import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import lexing.Loc;
import util.LateInit;

import java.util.List;
import java.util.Set;

public record BuiltinTypeDef(BuiltinType builtin, String generifiedName, String generifiedDescriptor,
                             String generifiedRuntimeName, boolean isReferenceType, boolean hasSpecialConstructor, List<Type> generics, int index,
                             LateInit<List<? extends MethodDef>, CompilationException> localizedMethods,
                             LateInit<Set<Type>, CompilationException> localizedSupertypes,
                             LateInit<Type, CompilationException> localizedTrueSupertype) implements TypeDef {
    @Override
    public String name() {
        return generifiedName;
    }

    @Override
    public boolean isSubtype(Type other, TypePool pool) {
        try {
            return localizedSupertypes.get().contains(other);
        } catch (CompilationException e) {
            throw new IllegalStateException("Bug in setup for BuiltinType " + builtin.name() + ", failed to locate supertypes?");
        }
    }

    //Helper method for casting checks
    public boolean isNumeric() {
        return builtin == IntLiteralType.INSTANCE ||
                builtin == FloatLiteralType.INSTANCE ||
                builtin instanceof IntegerType ||
                builtin instanceof FloatType;
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
    public void checkCode() throws CompilationException {
        
    }

    @Override
    public List<? extends MethodDef> getMethods() throws CompilationException {
        return localizedMethods.get();
    }

    @Override
    public List<? extends FieldDef> getFields() throws CompilationException {
        throw new IllegalStateException("Builtin type fields not yet implemented");
    }

    @Override
    public String getDescriptor() {
        return generifiedDescriptor;
    }

    @Override
    public String getRuntimeName() {
        return generifiedRuntimeName;
    }

    @Override
    public boolean extensible() {
        return false;
    }
}
