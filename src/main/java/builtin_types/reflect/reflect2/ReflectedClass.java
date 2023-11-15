package builtin_types.reflect.reflect2;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.reflect.annotations.Rename;
import lexing.Loc;
import org.objectweb.asm.Type;
import util.ListUtils;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

/**
 * A class that's being reflected.
 * Notably, not a struct or enum.
 */
public class ReflectedClass implements BuiltinType, ReflectedBuiltin {


    public final Class<?> reflectedClass;
    private final String name, descriptor, runtimeName;
    private final boolean extensible;
    private final List<ReflectedClassMethod> methods;
    private final List<ReflectedField> fields;

    public ReflectedClass(Class<?> classToReflect) {
        //Store basic data
        reflectedClass = classToReflect;
        if (classToReflect.isAnnotationPresent(Rename.class))
            name = classToReflect.getAnnotation(Rename.class).value();
        else
            name = classToReflect.getSimpleName();
        descriptor = Type.getDescriptor(classToReflect);
        runtimeName = Type.getInternalName(classToReflect);
        extensible = !Modifier.isFinal(classToReflect.getModifiers());
        fields = ListUtils.filter(ListUtils.map(List.of(reflectedClass.getFields()), ReflectedField::of), Objects::nonNull);
        methods = ListUtils.filter(ListUtils.map(List.of(reflectedClass.getMethods()), ReflectedClassMethod::of), Objects::nonNull);
    }

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> typeGenerics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        return ListUtils.map(methods, m -> m.get(checker, typeGenerics, instantiationLoc, cause));
    }

    public List<FieldDef> getFields(TypeChecker checker, List<TypeDef> typeGenerics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        return List.of();
    }

    @Override
    public Class<?> getJavaClass() {
        return reflectedClass;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String runtimeName(TypeChecker checker, List<TypeDef> generics) {
        return runtimeName;
    }

    @Override
    public boolean nameable() {
        return true;
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return List.of(descriptor);
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return descriptor;
    }

    @Override
    public boolean isReferenceType(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public boolean isPlural(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public boolean extensible(TypeChecker checker, List<TypeDef> generics) {
        return extensible;
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        return 1;
    }

}
