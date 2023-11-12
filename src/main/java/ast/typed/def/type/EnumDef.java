package ast.typed.def.type;

import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EnumDef implements TypeDef {

    public final Loc loc;
    public final int numElements;
    private final String name, descriptor;
    private final ArrayList<MethodDef> methods;
    private final List<FieldDef> fields;

    public EnumDef(Loc loc, String name, int numElements, List<FieldDef> fieldDefs, List<MethodDef> methodDefs) {
        this.loc = loc;
        this.name = name;
        this.numElements = numElements;
        this.fields = fieldDefs;
        this.methods = new ArrayList<>(methodDefs);

        if (numElements > 1 << 16)
            descriptor = "I";
        else if (numElements > 1 << 8)
            descriptor = "S";
        else
            descriptor = "B";
    }

    @Override
    public void checkCode() throws CompilationException {
        for (MethodDef methodDef : methods)
            methodDef.checkCode();
        //TODO: Check static code...
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
        return false;
    }

    @Override
    public boolean isPlural() {
        return false;
    }

    @Override
    public boolean extensible() {
        return false;
    }

    @Override
    public int stackSlots() {
        return 1;
    }

    @Override
    public Set<TypeDef> typeCheckingSupertypes() {
        return Set.of();
    }

    @Override
    public TypeDef inheritanceSupertype() {
        return null;
    }

    @Override
    public List<String> getDescriptor() {
        return List.of(descriptor);
    }

    @Override
    public String getReturnTypeDescriptor() {
        return descriptor;
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
    public List<FieldDef> fields() {
        return fields;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypeDef typeDef)
            return this == typeDef.get();
        return false;
    }

}
