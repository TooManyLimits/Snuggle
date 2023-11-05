package ast.typed.def.type;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An indirect wrapper over another TypeDef.
 * This layer of indirection is useful when loops happen while instantiating TypeDefs
 * with generics.
 */
public class IndirectTypeDef implements TypeDef {

    private TypeDef other = null;

    public void fill(TypeDef other) {
        if (this.other != null)
            throw new IllegalStateException("Attempt to fill the same IndirectTypeDef multiple times! Bug in compiler, please report!");
        this.other = other;
    }

    public TypeDef check() {
        if (this.other == null)
            throw new IllegalStateException("Attempt to use IndirectTypeDef before it was filled - bug in compiler, please report!");
        return this.other;
    }

    @Override
    public TypeDef get() {
        TypeDef cur = this;
        while (cur instanceof IndirectTypeDef ind)
            cur = ind.check();
        return cur;
    }

    @Override
    public void checkCode() throws CompilationException {
        check().checkCode();
    }

    @Override
    public BuiltinType builtin() { return check().builtin(); }
    @Override
    public boolean isNumeric() {
        return check().isNumeric();
    }
    @Override
    public boolean hasSpecialConstructor() {
        return check().hasSpecialConstructor();
    }
    @Override
    public TypeDef compileTimeToRuntimeConvert(TypeDef thisType, Loc loc, TypeChecker checker) throws CompilationException {
        return check().compileTimeToRuntimeConvert(thisType, loc, checker);
    }

    @Override
    public String name() {
        return check().name();
    }
    @Override
    public String runtimeName() {
        return check().runtimeName();
    }

    @Override
    public boolean isReferenceType() {
        return check().isReferenceType();
    }

    @Override
    public boolean isPlural() {
        return check().isPlural();
    }

    @Override
    public boolean extensible() {
        return check().extensible();
    }

    @Override
    public int stackSlots() {
        return check().stackSlots();
    }

    @Override
    public List<String> getDescriptor() {
        return check().getDescriptor();
    }

    @Override
    public String getReturnTypeDescriptor() {
        return check().getReturnTypeDescriptor();
    }

    @Override
    public Set<TypeDef> typeCheckingSupertypes() {
        return check().typeCheckingSupertypes();
    }

    @Override
    public TypeDef inheritanceSupertype() {
        return check().inheritanceSupertype();
    }

    @Override
    public List<FieldDef> fields() {
        return check().fields();
    }

    @Override
    public List<MethodDef> methods() {
        return check().methods();
    }

    @Override
    public int hashCode() {
        return check().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypeDef otherDef)
            return Objects.equals(get(), otherDef.get());
        return false;
    }

    @Override
    public String toString() {
        return get().toString();
    }
}
