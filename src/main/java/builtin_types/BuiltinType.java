package builtin_types;

import ast.passes.TypeChecker;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import ast.typed.def.method.MethodDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;
import java.util.Set;

public interface BuiltinType {

    //Methods for filling in info in the BuiltinTypeDef:
    String name();
    default String genericName(TypeChecker checker, List<TypeDef> generics) {
        if (generics.size() != numGenerics())
            throw new IllegalStateException("Somehow getting generic name with wrong number of generics? Bug in compiler, please report!");
        return TypeResolvedTypeDef.instantiateName(name(), generics);
    }

    /**
     * Set to false for types that cannot be referred to by name.
     * Example: IntLiteral
     * For most types, those with names, this returns true, which
     * is why it's default.
     */
    default boolean nameable() { return true; }
    //The name at runtime for this type. For example, name() -> "Obj", runtimeName() -> "java/lang/Object"
    default String runtimeName(TypeChecker checker, List<TypeDef> generics) { return "builtin_structs/" + genericName(checker, generics); }
    List<String> descriptor(TypeChecker checker, List<TypeDef> generics); //The descriptor for this type.
    String returnDescriptor(TypeChecker checker, List<TypeDef> generics); //The descriptor for this type in return position. Usually the same as the regular descriptor.
    boolean isReferenceType(TypeChecker checker, List<TypeDef> generics); //Is this a reference type?
    boolean isPlural(TypeChecker checker, List<TypeDef> generics); //Is this type plural?
    boolean extensible(TypeChecker checker, List<TypeDef> generics); //Can this be extended?
    default boolean hasSpecialConstructor(TypeChecker checker, List<TypeDef> generics) { return false; } //Does this have a special constructor, or will a regular NEW bytecode work?
    default boolean shouldGenerateStructClassAtRuntime(TypeChecker checker, List<TypeDef> generics) { return false; } //Generally false, except for a few cases
    int stackSlots(TypeChecker checker, List<TypeDef> generics); //Number of stack slots this takes

    /**
     * The number of generics this has. Usually 0,
     * sometimes more, in cases like Array and Option.
     */
    default int numGenerics() { return 0; }

    /**
     * Get the type-checking supertypes of this type. Default empty.
     */
    default Set<TypeDef> getTypeCheckingSupertypes(TypeChecker checker, List<TypeDef> generics) { return Set.of(); }

    /**
     * Supertype which inheritance affects. Some types have type-checking supertypes, but don't inherit from them.
     * (example: IntLiteral is a subtype of u32, i8, etc. but it does not inherit from them)
     */
    default TypeDef getInheritanceSupertype(TypeChecker checker, List<TypeDef> generics) { return null; }

    default TypeDef compileTimeToRuntimeConvert(TypeDef thisType, Loc loc, TypeDef.InstantiationStackFrame cause, TypeChecker checker) throws CompilationException {
        return thisType;
    }

    /**
     * Create all the methods for this, given the mapping
     * __within the current compilation__ of BuiltinType -> TypeDef.
     * It's important to note that:
     * - BuiltinType is a GLOBAL concept, not connected to any specific compilation instance.
     * - TypeDef is a LOCAL concept, containing information that's only meaningful
     *   inside the compilation instance it's a part of.
     * - We need this map between the two in order for the GLOBAL concept to generate methods
     *   which operate on LOCAL types.
     */
    default List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) { return List.of(); }

    default List<FieldDef> getFields(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) { return List.of(); }

//    /**
//     * Return the java descriptor for this type.
//     * char -> C
//     * byte -> B
//     * short -> S
//     * int -> I
//     * long -> J
//     * boolean -> Z
//     * float -> F
//     * double -> D
//     * reference type -> "L" + (fully qualified class methodName) + ";"
//     */
//    String getDescriptor(List<Type> generics, TypePool pool);
//    String getRuntimeName(List<Type> generics, TypePool pool);
//    boolean isReferenceType(List<Type> generics, TypePool pool);
//    boolean extensible();
//    default boolean hasSpecialConstructor(List<Type> generics, TypePool pool) { return false; }
//
//    /**
//     * Attempt to convert this type into a storable type.
//     * If this type is already storable, just return thisType.
//     * Examples of non-storable types are literals, like
//     * StringLiteral, IntLiteral, etc.
//     */
//    default Type toStorable(Type thisType, Loc loc, TypePool pool) throws CompilationException {
//        return thisType;
//    }

}
