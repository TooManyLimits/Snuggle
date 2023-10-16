package builtin_types;

import ast.typed.Type;
import exceptions.CompilationException;
import ast.passes.TypePool;
import ast.typed.def.method.MethodDef;
import lexing.Loc;

import java.util.List;
import java.util.Set;

public interface BuiltinType {
    String name();

    /**
     * Set to false for types that cannot be referred to by methodName.
     * Example: IntLiteral
     * For most types, those with names, this returns true, which
     * is why it's default.
     */
    default boolean nameable() { return true; }

    /**
     * Get the type-checking supertypes of this type
     */
    default Set<Type> getSupertypes(TypePool pool) throws CompilationException { return Set.of(); }

    /**
     * "True" supertype, that which this can call methods of seamlessly
     */
    default Type getTrueSupertype(TypePool pool) throws CompilationException { return null; }

    /**
     * Create all the methods for this, given the mapping
     * __within the current compilation__ of BuiltinType -> Type.
     * It's important to note that:
     * - BuiltinType is a GLOBAL concept, not connected to any specific compilation instance.
     * - Type is a LOCAL concept, containing information that's only meaningful
     *   inside the compilation instance it's a part of.
     * - We need this map between the two in order for the GLOBAL concept to generate methods
     *   which operate on LOCAL types.
     */
    default List<? extends MethodDef> getMethods(TypePool pool) throws CompilationException {return List.of();}

    /**
     * Return the java descriptor for this type.
     * char -> C
     * byte -> B
     * short -> S
     * int -> I
     * long -> J
     * boolean -> Z
     * float -> F
     * double -> D
     * reference type -> "L" + (fully qualified class methodName) + ";"
     */
    String getDescriptor(int index);

    /**
     * Attempt to convert this type into a storable type.
     * If this type is already storable, just return thisType.
     * Examples of non-storable types are literals, like
     * StringLiteral, IntLiteral, etc.
     */
    default Type toStorable(Type thisType, Loc loc, TypePool pool) throws CompilationException {
        return thisType;
    }

}
