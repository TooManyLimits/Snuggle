package ast.typed.def.type;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import builtin_types.types.numbers.FloatLiteralType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntLiteralType;
import builtin_types.types.numbers.IntegerType;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BuiltinTypeDef implements TypeDef, FromTypeHead {

    private final BuiltinType builtin;
    public final int typeHeadId;
    public final List<TypeDef> generics;
    private final String name, runtimeName, returnDescriptor;
    private final List<String> descriptor;
    private final boolean isReferenceType, isPlural, extensible, hasSpecialConstructor, shouldGenerateStructClassAtRuntime;
    private final int stackSlots;
    private final Set<TypeDef> typeCheckingSupertypes;
    private final TypeDef inheritanceSupertype;
    private final List<FieldDef> fields;
    private final ArrayList<MethodDef> methods;

    public BuiltinTypeDef(BuiltinType builtinType, int typeHeadId, List<TypeDef> generics, TypeChecker checker, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        this.builtin = builtinType;
        this.typeHeadId = typeHeadId;
        this.generics = List.copyOf(generics);
        this.name = builtinType.genericName(checker, generics);
        this.descriptor = builtinType.descriptor(checker, generics);
        this.runtimeName = builtinType.runtimeName(checker, generics);
        this.returnDescriptor = builtinType.returnDescriptor(checker, generics);
        this.isReferenceType = builtinType.isReferenceType(checker, generics);
        this.isPlural = builtinType.isPlural(checker, generics);
        this.extensible = builtinType.extensible(checker, generics);
        this.hasSpecialConstructor = builtinType.hasSpecialConstructor(checker, generics);
        this.shouldGenerateStructClassAtRuntime = builtinType.shouldGenerateStructClassAtRuntime(checker, generics);
        this.stackSlots = builtinType.stackSlots(checker, generics);
        this.typeCheckingSupertypes = builtinType.getTypeCheckingSupertypes(checker, generics);
        this.inheritanceSupertype = builtinType.getInheritanceSupertype(checker, generics);
        this.fields = builtinType.getFields(checker, generics, instantiationLoc, cause);
        this.methods = new ArrayList<>(builtinType.getMethods(checker, generics, instantiationLoc, cause));
    }

    @Override
    public int getTypeHeadId() {
        return typeHeadId;
    }

    //Whether this should generate a struct class at runtime.
    //Usually no, except for certain builtin cases, such as
    //Option<T> where T is not a reference type.
    public boolean shouldGenerateStructClassAtRuntime() {
        return shouldGenerateStructClassAtRuntime;
    }

    @Override
    public List<TypeDef> generics() {
        return generics;
    }

    @Override
    public BuiltinType builtin() {
        return builtin;
    }

    @Override
    public boolean isNumeric() {
        return builtin == IntLiteralType.INSTANCE ||
                builtin == FloatLiteralType.INSTANCE ||
                builtin instanceof IntegerType ||
                builtin instanceof FloatType;
    }

    //Whether this type has a special constructor situation.
    @Override
    public boolean hasSpecialConstructor() {
        return hasSpecialConstructor;
    }

    @Override
    public void checkCode() throws CompilationException {
        //No code to check here
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String runtimeName() {
//        if (runtimeName == null)
//            throw new IllegalStateException("Should never be asking " + name + " for runtime name.");
        return runtimeName;
    }

    @Override
    public boolean isReferenceType() {
        return isReferenceType;
    }

    @Override
    public boolean isPlural() {
        return isPlural;
    }

    @Override
    public boolean extensible() {
        return extensible;
    }

    @Override
    public int stackSlots() {
        if (stackSlots < 0)
            throw new IllegalStateException("Should never be asking " + name + " for stack slots.");
        return stackSlots;
    }

    @Override
    public List<String> getDescriptor() {
        if (descriptor == null)
            throw new IllegalStateException("Should never be asking " + name + " for descriptor.");
        return descriptor;
    }

    @Override
    public String getReturnTypeDescriptor() {
        if (returnDescriptor == null)
            throw new IllegalStateException("Should never be asking " + name + " for descriptor.");
        return returnDescriptor;
    }

    @Override
    public Set<TypeDef> typeCheckingSupertypes() {
        return typeCheckingSupertypes;
    }

    @Override
    public TypeDef inheritanceSupertype() {
        return inheritanceSupertype;
    }

    @Override
    public TypeDef compileTimeToRuntimeConvert(TypeDef thisType, Loc loc, TypeDef.InstantiationStackFrame cause, TypeChecker checker) throws CompilationException {
        return builtin.compileTimeToRuntimeConvert(thisType, loc, cause, checker);
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
