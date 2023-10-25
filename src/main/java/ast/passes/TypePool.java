package ast.passes;

import builtin_types.reflect.ReflectedBuiltin;
import exceptions.compile_time.CompilationException;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import ast.type_resolved.prog.TypeResolvedAST;
import ast.typed.Type;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import util.ListUtils;

import java.util.*;

/**
 * Manages all the TypeDef used in the Type Checking phase.
 * A Type is a handle into the TypePool.
 * TypeDef are created by instantiating TypeResolvedTypeDef with given generics.
 */
public class TypePool {

    public TypePool(TypeChecker checker, TypeResolvedAST ast) throws CompilationException {
        this.checker = checker;
        this.startingTypeDefs = ast.typeDefs();
        this.builtinTypeMap = ast.builtinIds();
        this.reflectedTypeMap = ast.reflectedBuiltins();
    }

    private final List<TypeResolvedTypeDef> startingTypeDefs;
    private final IdentityHashMap<BuiltinType, Integer> builtinTypeMap;
    private final IdentityHashMap<Class<?>, ReflectedBuiltin> reflectedTypeMap;
    private final TypeChecker checker;

    private final List<TypeDef> instantiatedTypeDefs = new ArrayList<>();

    private final Map<Integer, Map<List<Type>, Type>> cache = new HashMap<>();
    private final Map<Type, ResolvedType.Basic> cacheInverse = new HashMap<>();

    //Fetch a annotatedType from the cache, or compute it if it doesn't exist yet
    public Type getOrInstantiateType(ResolvedType resolvedType, List<Type> typeGenerics) throws CompilationException {
        if (resolvedType instanceof ResolvedType.Basic basic) {
            //Get from cache if present
            List<Type> convertedGenerics = ListUtils.map(basic.generics(), g -> this.getOrInstantiateType(g, typeGenerics));
            Map<List<Type>, Type> tMap = cache.get(basic.index());
            if (tMap != null) {
                Type t = tMap.get(convertedGenerics);
                if (t != null)
                    return t;
            }

            //For basic types: instantiate the annotatedType def. Finally store in cache.

            //Little bit of playing around in order to avoid recursion issues giving the same index to multiple types
            int index = instantiatedTypeDefs.size();
            instantiatedTypeDefs.add(null);
            //Add to the cache before instantiating it, to avoid stack overflow when a generic class references itself
            Type resultType = new Type.Basic(index);
            cache.computeIfAbsent(basic.index(), x -> new HashMap<>()).put(convertedGenerics, resultType);
            cacheInverse.put(resultType, basic);
            //Instantiate the type
            TypeDef instantiated = startingTypeDefs.get(basic.index()).instantiate(index, this.checker, convertedGenerics);
            instantiatedTypeDefs.set(index, instantiated);
            //And return
            return resultType;
        } else if (resolvedType instanceof ResolvedType.Generic generic) {
            //For generics, method generics should stay generic, while annotatedType generics should be flattened
            if (generic.isMethod())
                return new Type.Generic(generic.index());
            else
                return typeGenerics.get(generic.index());
        } else {
            throw new IllegalStateException("Unexpected annotatedType, bug in compiler, please report!");
        }
    }

    public List<TypeDef> getFinalTypeDefs() {
        return instantiatedTypeDefs;
    }

    public ResolvedType getBuiltinResolvedType(BuiltinType builtin, List<ResolvedType> generics) {
        Integer b = builtinTypeMap.get(builtin);
        if (b == null)
            throw new IllegalStateException("Attempted to get ResolvedType for builtin " + builtin.name() + ", but it was not registered? Bug in compiler, please report!");
        return new ResolvedType.Basic(b, generics);
    }

    public Type getBasicBuiltin(BuiltinType builtin) throws CompilationException {
        return getOrInstantiateType(getBuiltinResolvedType(builtin, List.of()), List.of());
    }

    public Type getReflectedBuiltin(Class<?> c) throws CompilationException {
        BuiltinType b = reflectedTypeMap.get(c);
        if (b == null)
            throw new IllegalStateException("Attempted to get reflected builtin for class " + c.getName() + ", but it was not registered? Bug in compiler, please report!");
        return getBasicBuiltin(b);
    }

    public ResolvedType getInverse(Type t) {
        ResolvedType inverse = cacheInverse.get(t);
        if (inverse == null)
            throw new IllegalStateException("Somehow getting inverse of type that doesn't exist in inverse map? Bug in compiler, please report!");
        return inverse;
    }

    public Type getGenericBuiltin(BuiltinType builtinType, List<Type> generics) throws CompilationException {
        //Invert the generics provided, and then getOrInstantiateType with it
        List<ResolvedType> inverseGenerics = ListUtils.map(generics, this::getInverse);
        return getOrInstantiateType(getBuiltinResolvedType(builtinType, inverseGenerics), List.of());
    }

    //Get the TypeDef from this Type
    public TypeDef getTypeDef(Type t) {
        if (t instanceof Type.Basic basic)
            return instantiatedTypeDefs.get(basic.index());
        throw new IllegalStateException("Attempt to getTypeDef with generic Type? Bug in compiler, please report!");
    }


}
