package ast.passes.typeinference;

import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;

import java.util.List;
import java.util.Set;

public record UnknownGenericType(int index) implements TypeDef {

    @Override
    public List<TypeDef> generics() {
        throw new IllegalStateException("Unknown generic typedefs do not implement methods");
    }

    @Override
    public String name() {
        return "##_UNKNOWN_GENERIC_" + index + "_##";
    }

    @Override
    public String runtimeName() {
        return name();
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
        throw new IllegalStateException("Unknown generic typedefs do not implement methods");
    }

    @Override
    public int stackSlots() {
        return -1;
    }

    @Override
    public Set<TypeDef> typeCheckingSupertypes() throws CompilationException {
        throw new IllegalStateException("Unknown generic typedefs do not implement methods");
    }

    @Override
    public TypeDef inheritanceSupertype() throws CompilationException {
        throw new IllegalStateException("Unknown generic typedefs do not implement methods");
    }

    @Override
    public List<FieldDef> fields() {
        return List.of();
//        throw new IllegalStateException("Unknown generic typedefs do not implement methods");
    }

    @Override
    public List<MethodDef> methods() {
        return List.of();
//        throw new IllegalStateException("Unknown generic typedefs do not implement methods");
    }

    @Override
    public void addMethod(MethodDef newMethod) {
        throw new IllegalStateException("Unknown generic typedefs do not implement methods");
    }

    @Override
    public List<String> getDescriptor() {
        return List.of(name());
    }

    @Override
    public String getReturnTypeDescriptor() {
        return name();
    }

    @Override
    public void checkCode() throws CompilationException {
        throw new IllegalStateException("Unknown generic typedefs do not implement methods");
    }
}
