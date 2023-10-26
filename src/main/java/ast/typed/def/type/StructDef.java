package ast.typed.def.type;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.field.FieldDef;
import ast.typed.def.field.SnuggleFieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import compile.Compiler;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;

public record StructDef(Loc loc, int index, String name, List<SnuggleMethodDef> methods, List<SnuggleFieldDef> fields) implements SnuggleTypeDef {

    @Override
    public byte[] compile(Compiler compiler) throws CompilationException {
        return new byte[0];
    }



    @Override
    public List<? extends MethodDef> getMethods() throws CompilationException {
        return null;
    }

    @Override
    public List<? extends FieldDef> getFields() throws CompilationException {
        return null;
    }

    @Override
    public void checkCode() throws CompilationException {

    }

    @Override
    public Type toStorable(Type thisType, Loc loc, TypePool pool) throws CompilationException {
        return thisType;
    }

    @Override
    public boolean isSubtype(Type other, TypePool pool) {
        return false; //Structs are not subtypes of anything
    }

    @Override
    public Type trueSupertype() throws CompilationException {
        return null; //Structs have no inheritance
    }

    @Override
    public String getDescriptor() {
        return null;
    }

    @Override
    public String getRuntimeName() {
        return null;
    }

    @Override
    public boolean extensible() {
        return false; //Structs cannot be extended
    }

    @Override
    public boolean isReferenceType() {
        return false; //Structs are not reference types
    }
}
