package ast.typed.def.type;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import exceptions.compile_time.CompilationException;

import java.util.List;
import java.util.Set;

/**
 * These only really exist to be replaced with non-generic TypeDef
 * when instantiating a generic method.
 * @param index The index of the generic in the method.
 */
public record GenericTypeDef(int index) implements TypeDef {

    @Override
    public void checkCode() throws CompilationException {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public String name() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public String runtimeName() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public boolean isReferenceType() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public boolean isPlural() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public boolean extensible() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public int stackSlots() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public List<String> getDescriptor() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public String getReturnTypeDescriptor() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public Set<TypeDef> typeCheckingSupertypes() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public TypeDef inheritanceSupertype() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public List<FieldDef> fields() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public List<MethodDef> methods() {
        throw new IllegalStateException("TypeDef interface methods not defined on GenericTypeDef! Shouldn't be calling this - Bug in compiler, please report!");
    }

    @Override
    public int hashCode() {
        throw new IllegalStateException("Bug in compiler - shouldn't be hashing generic type def!");
    }
}
