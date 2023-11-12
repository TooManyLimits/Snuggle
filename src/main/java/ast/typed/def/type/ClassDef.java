package ast.typed.def.type;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.LateInit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ClassDef implements TypeDef {

    public final Loc loc;
    private final String name;
    private final LateInit<TypeDef, CompilationException> supertype;
    private final List<FieldDef> fields;
    private final ArrayList<MethodDef> methods;

    public ClassDef(Loc loc, String name, LateInit<TypeDef, CompilationException> supertype, List<FieldDef> fields, List<MethodDef> methods) {
        this.loc = loc;
        this.name = "snuggle/" + loc.fileName() + "/" + name;
        this.supertype = supertype;
        this.fields = fields;
        this.methods = new ArrayList<>(methods);
    }

    @Override
    public void checkCode() throws CompilationException {
        supertype.get();
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
        return true; //ClassDef are reference topLevelTypes
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
    public Set<TypeDef> typeCheckingSupertypes() throws CompilationException {
        return Set.of(supertype.get());
    }

    @Override
    public TypeDef inheritanceSupertype() throws CompilationException {
        return supertype.get();
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
    public void addMethod(MethodDef newMethod) {
        methods.add(newMethod);
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
