package builtin_types.reflect;

import ast.passes.TypeChecker;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.reflect.annotations.SnuggleBlacklist;
import builtin_types.reflect.annotations.SnuggleType;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import builtin_types.types.ObjType;
import lexing.Loc;
import util.ListUtils;
import util.throwing_interfaces.ThrowingFunction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ReflectedBuiltin implements BuiltinType {

    public final Class<?> reflectedClass;
    public final String name, descriptor, runtimeName;
    public final boolean nameable;
    private final ArrayList<ReflectedMethod> reflectedMethods;
    private final ThrowingFunction<TypeChecker, TypeDef, RuntimeException> supertypeGetter;


    public ReflectedBuiltin(Class<?> reflectedClass) {
        //Fetch the @SnuggleType annotation, error if not found
        SnuggleType typeAnnotation = reflectedClass.getAnnotation(SnuggleType.class);
        if (typeAnnotation == null)
            throw new IllegalArgumentException("Attempt to reflect class " + reflectedClass.getName() + ", but it doesn't have an @SnuggleType annotation.");
        //Fill in basic fields
        this.reflectedClass = reflectedClass;
        this.name = typeAnnotation.name();
        this.nameable = typeAnnotation.nameable();
        this.descriptor = typeAnnotation.descriptor().isEmpty() ? org.objectweb.asm.Type.getDescriptor(reflectedClass) : typeAnnotation.descriptor();
        this.runtimeName = descriptor.substring(1, descriptor.length() - 1);
        //Get supertypeGetter
        Class<?> supertype = typeAnnotation.forceSupertype() ? typeAnnotation.supertype() : reflectedClass.getSuperclass();
        if (supertype == Object.class)
            supertypeGetter = pool -> pool.getBasicBuiltin(ObjType.INSTANCE);
        else
            supertypeGetter = pool -> pool.getReflectedBuiltin(supertype);

        boolean isWhitelistDefault = reflectedClass.getAnnotation(SnuggleWhitelist.class) != null;

        //Generate methods
        reflectedMethods = new ArrayList<>();
        for (Method method : reflectedClass.getDeclaredMethods()) {
            //Only add whitelisted methods
            boolean hasWhitelist = method.getAnnotation(SnuggleWhitelist.class) != null;
            if (isWhitelistDefault && hasWhitelist)
                throw new IllegalStateException("Warning: method has @SnuggleWhitelist annotation" +
                        " in class that is already @SnuggleWhitelist! " +
                        "There is likely a mistake in your environment!");
            //If the method is whitelisted, or whitelist is default, and it's not blacklisted, then add it.
            if (hasWhitelist || (isWhitelistDefault && method.getAnnotation(SnuggleBlacklist.class) == null))
                reflectedMethods.add(new ReflectedMethod(method));
        }
    }

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        return ListUtils.map(reflectedMethods, m -> m.get(checker, instantiationLoc, cause));
    }

    @Override
    public String name() {
        return name;
    }
    @Override
    public boolean nameable() {
        return nameable;
    }

    @Override
    public String runtimeName(TypeChecker checker, List<TypeDef> generics) {
        return runtimeName;
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
        return true;
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        return 1;
    }

    @Override
    public int numGenerics() {
        return 0;
    }

    @Override
    public TypeDef getInheritanceSupertype(TypeChecker checker, List<TypeDef> generics) {
        return supertypeGetter.apply(checker);
    }

    @Override
    public Set<TypeDef> getTypeCheckingSupertypes(TypeChecker checker, List<TypeDef> generics) {
        return Set.of(getInheritanceSupertype(checker, generics));
    }

}
