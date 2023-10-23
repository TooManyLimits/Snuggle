package ast.typed.def.type;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.field.FieldDef;
import ast.typed.def.field.SnuggleFieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import compile.Compiler;
import compile.NameHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.ClassWriter;

import java.util.List;

/**
 * InstantiationLoc is the location where this TypeDef was instantiated. Useful for error reporting.
 */
public record ClassDef(Loc loc, int index, String name, Type supertype, List<SnuggleMethodDef> methods, List<SnuggleFieldDef> fields) implements SnuggleTypeDef {

    @Override
    public Type toStorable(Type thisType, Loc loc, TypePool pool) {
        //ClassDef always storable.
        return thisType;
    }

    @Override
    public boolean isSubtype(Type other, TypePool pool) {
        if (this == pool.getTypeDef(other))
            return true;
        if (supertype == null)
            return false;
        return supertype.isSubtype(other, pool);
    }

    @Override
    public Type trueSupertype() {
        return supertype;
    }

    @Override
    public void checkCode() throws CompilationException {
        for (SnuggleMethodDef def : methods)
            def.body().get();
        for (SnuggleFieldDef def : fields)
            if (def.initializer() != null)
                def.initializer().get();
    }

    @Override
    public List<? extends MethodDef> getMethods() {
        return methods;
    }

    @Override
    public List<? extends FieldDef> getFields() throws CompilationException { return fields; }

    @Override
    public byte[] compile(Compiler compiler) throws CompilationException {
        //Figure out our supertype's name
        String supertypeName = compiler.getTypeDef(supertype).getRuntimeName();
        //Create the writer
        ClassWriter writer = NameHelper.generateClassWriter(NameHelper.getSnuggleClassName(index), supertypeName);
        Type thisType = new Type.Basic(index);
        //Write all the methods
        for (SnuggleFieldDef fieldDef : fields)
            fieldDef.compile(thisType, compiler, writer);
        for (SnuggleMethodDef methodDef : methods)
            methodDef.compile(thisType, compiler, writer);
        //End class writer and return bytes
        writer.visitEnd();
        return writer.toByteArray();
    }

    @Override
    public String getDescriptor() {
        return "L" + getRuntimeName() + ";";
    }

    @Override
    public String getRuntimeName() {
        return NameHelper.getSnuggleClassName(index);
    }

    @Override
    public boolean extensible() {
        return true;
    }
}
