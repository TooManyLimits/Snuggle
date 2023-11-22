package ast.typed.def.type;

import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StructDef implements TypeDef, FromTypeHead {

    public final Loc loc;
    private final String name, returnTypeDescriptor;
    private final List<String> descriptor;
    private final List<FieldDef> fields;
    private final ArrayList<MethodDef> methods;
    private final List<TypeDef> generics;
    private final int typeHeadId;
    private final int stackSlots;

    public StructDef(Loc loc, String name, int typeHeadId, List<TypeDef> generics, List<FieldDef> fields, List<MethodDef> methods) {
        this.loc = loc;
        this.name = "snuggle/" + loc.fileName() + "/" + name;
        this.fields = fields;
        this.methods = new ArrayList<>(methods);
        this.typeHeadId = typeHeadId;
        this.generics = generics;
        this.stackSlots = fields.stream().filter(f -> !f.isStatic()).map(f -> f.type().stackSlots()).reduce(Integer::sum).orElse(0);
        this.descriptor = ListUtils.join(ListUtils.map(nonStaticFields(), f -> f.type().getDescriptor()));
        this.returnTypeDescriptor = "V";
    }

    @Override
    public void checkCode() throws CompilationException {
        for (FieldDef field : fields)
            field.checkCode();
        for (MethodDef method : methods)
            method.checkCode();
    }

    @Override
    public int getTypeHeadId() {
        return typeHeadId;
    }

    @Override
    public List<TypeDef> generics() {
        return generics;
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
        return true;
    }

    @Override
    public boolean extensible() {
        return false;
    }

    @Override
    public int stackSlots() {
        return stackSlots;
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
    public List<String> getDescriptor() {
        return descriptor;
    }

    @Override
    public String getReturnTypeDescriptor() {
        return returnTypeDescriptor;
    }

    @Override
    public boolean hasSpecialConstructor() {
        return true;
    }
}
