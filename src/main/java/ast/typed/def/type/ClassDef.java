package ast.typed.def.type;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ClassDef implements TypeDef {

    public final Loc loc;
    private final String name;
    private final TypeDef supertype;
    private final List<FieldDef> fields;
    private final List<MethodDef> methods;

    public ClassDef(Loc loc, String name, TypeDef supertype, List<FieldDef> fields, List<MethodDef> methods) {
        this.loc = loc;
        this.name = "snuggle/" + loc.fileName() + "/" + name;
        this.supertype = supertype;
        this.fields = fields;
        this.methods = methods;
    }

    @Override
    public void checkCode() throws CompilationException {
        for (FieldDef field : fields)
            field.checkCode();
        for (MethodDef method : methods)
            method.checkCode();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String runtimeName() {
        return name;
    }

    @Override
    public boolean isReferenceType() {
        return true; //ClassDef are reference types
    }

    @Override
    public boolean isPlural() {
        return false; //ClassDef are not plural
    }

    @Override
    public boolean extensible() {
        return true; //ClassDef are extensible
    }

    @Override
    public int stackSlots() {
        return 1;
    }

    @Override
    public List<String> getDescriptor() {
        return List.of("L" + name + ";");
    }

    @Override
    public String getReturnTypeDescriptor() {
        return "L" + name + ";";
    }

    @Override
    public Set<TypeDef> typeCheckingSupertypes() {
        return Set.of(supertype);
    }

    @Override
    public TypeDef inheritanceSupertype() {
        return supertype;
    }

    @Override
    public List<FieldDef> fields() {
        return fields;
    }

    @Override
    public List<MethodDef> methods() {
        return methods;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypeDef typeDef)
            return this == typeDef.get();
        return false;
    }
}
