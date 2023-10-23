package builtin_types.reflect;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import builtin_types.reflect.annotations.SnuggleBlacklist;
import builtin_types.reflect.annotations.SnuggleType;
import builtin_types.reflect.annotations.SnuggleWhitelist;
import builtin_types.types.ObjType;
import exceptions.compile_time.CompilationException;
import util.ListUtils;
import util.ThrowingFunction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ReflectedBuiltin implements BuiltinType {

    public final Class<?> reflectedClass;
    public final String name, descriptor, runtimeName;
    public final boolean nameable;
    private final ArrayList<ReflectedMethod> reflectedMethods;
    private final ThrowingFunction<TypePool, Type, CompilationException> supertypeGetter;


    public ReflectedBuiltin(Class<?> reflectedClass) {
        //Fetch the @SnuggleType annotation, error if not found
        SnuggleType typeAnnotation = reflectedClass.getAnnotation(SnuggleType.class);
        if (typeAnnotation == null)
            throw new IllegalArgumentException("Attempt to reflect class " + reflectedClass.getName() + ", but it doesn't have an @SnuggleType annotation.");
        //Fill in basic fields
        this.reflectedClass = reflectedClass;
        this.name = typeAnnotation.name();
        this.nameable = typeAnnotation.nameable();
        this.descriptor = typeAnnotation.descriptor().isEmpty() ? ReflectionUtils.getDescriptor(reflectedClass) : typeAnnotation.descriptor();
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
    public String name() {
        return name;
    }
    @Override
    public boolean nameable() {
        return nameable;
    }

    @Override
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return descriptor;
    }

    @Override
    public Set<Type> getSupertypes(List<Type> generics, TypePool pool) throws CompilationException {
        return Set.of(supertypeGetter.apply(pool));
    }

    @Override
    public Type getTrueSupertype(List<Type> generics, TypePool pool) throws CompilationException {
        return supertypeGetter.apply(pool);
    }

    @Override
    public String getRuntimeName(List<Type> generics, TypePool pool) {
        return runtimeName;
    }

    @Override
    public boolean extensible() {
        return true;
    }

    @Override
    public List<? extends MethodDef> getMethods(List<Type> generics, TypePool pool) throws CompilationException {
        return ListUtils.map(reflectedMethods, m -> m.get(pool));
    }
}
