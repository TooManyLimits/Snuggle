package ast.typed.def.type;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.InterfaceMethodDef;
import builtin_types.types.ObjType;
import exceptions.compile_time.CompilationException;
import util.GenericStringUtil;
import util.ListUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FuncTypeDef implements TypeDef {

    public AtomicInteger implIndex = new AtomicInteger();
    public final List<TypeDef> paramTypes;
    public final TypeDef resultType;
    private final String name, runtimeName, descriptor;
    private final List<MethodDef> methods;
    private final TypeDef supertype;

    public FuncTypeDef(TypeChecker checker, List<TypeDef> paramTypes, TypeDef resultType) {
        supertype = checker.getBasicBuiltin(ObjType.INSTANCE); //Obj is the supertype
        name = GenericStringUtil.instantiateName("Func_", paramTypes) + "_to_" + resultType.name();
        runtimeName = "lambdas/" + name + "/base";
        descriptor = "L" + runtimeName + ";";
        methods = List.of(new InterfaceMethodDef(
                "invoke",
                0,
                false,
                this,
                paramTypes,
                resultType
        ));
        this.paramTypes = paramTypes;
        this.resultType = resultType;
    }

    @Override
    public List<TypeDef> generics() {
        return ListUtils.join(List.of(resultType), paramTypes);
    }

    @Override
    public void checkCode() throws CompilationException {
        for (MethodDef method : methods)
            method.checkCode();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String runtimeName() {
        return runtimeName;
    }

    @Override
    public boolean isReferenceType() {
        return true;
    }

    @Override
    public boolean isPlural() {
        return false;
    }

    @Override
    public boolean extensible() {
        return true;
    }

    @Override
    public int stackSlots() {
        return 1;
    }

    @Override
    public Set<TypeDef> typeCheckingSupertypes() throws CompilationException {
        return Set.of(supertype);
    }

    @Override
    public TypeDef inheritanceSupertype() throws CompilationException {
        return supertype;
    }

    @Override
    public List<FieldDef> fields() {
        return List.of();
    }

    @Override
    public List<MethodDef> methods() {
        return methods;
    }

    @Override
    public void addMethod(MethodDef newMethod) {
        throw new IllegalStateException("Tried to add method to FuncTypeDef? bug in compiler, please report");
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
    public boolean hasSpecialConstructor() {
        return true;
    }
}
