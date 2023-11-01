package ast.typed.def.type;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import builtin_types.types.OptionType;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.*;

public interface TypeDef {
    String name();
    String runtimeName(); //If this type is a reference type, and it's not generated by Snuggle, then this should be its runtime name. Example Obj -> java/lang/Object.
    boolean isReferenceType(); //If this TypeDef is a reference type. Mutually exclusive with isPlural.
    boolean isPlural(); //If this TypeDef is plural. Mutually exclusive with isReferenceType.
    boolean extensible(); //If this TypeDef is extensible. Mutually exclusive with isPlural.
    int stackSlots(); //The number of stack slots this takes up on the jvm. Usually 1, except in doubles, longs, or plural types.
    Set<TypeDef> typeCheckingSupertypes(); //The supertypes of this type, when used for the purpose of type-checking.
    TypeDef inheritanceSupertype(); //The supertype from which this can inherit methods. Null for types that don't inherit.
    List<FieldDef> fields(); //The fields of this type. In the case of plural types, describes the sub-types inside this.
    List<MethodDef> methods(); //The methods of this type.
    List<String> getDescriptor(); //The descriptor(s) of this type when not a return type. Multiple descriptors when isPlural().
    String getReturnTypeDescriptor(); //The descriptor of this type when it is a return type.

    //Type-check the method bodies.
    void checkCode() throws CompilationException;

    //Get the reduced form of this TypeDef. Only different for IndirectTypeDef.
    default TypeDef get() { return this; }

    //Get the corresponding builtin for this TypeDef. Usually null, except for
    //BuiltinTypeDef.
    default BuiltinType builtin() { return null; }

    default boolean isNumeric() { return false; } //Also false except for BuiltinTypeDef

    default boolean isOptionalReferenceType() {
        return builtin() == OptionType.INSTANCE && !get().isPlural();
    }

    default boolean hasSpecialConstructor() { return false; }

    /**
     * Attempt to convert this type into a runtime type.
     * If this type is already a runtime type, just return thisType.
     * Examples of compile-time types are literals, like StringLiteral, IntLiteral, etc.
     * Default is to return thisType, since the type is usually already a runtime type.
     */
    default TypeDef compileTimeToRuntimeConvert(TypeDef thisType, Loc loc, TypeChecker checker) throws CompilationException { return thisType; }

    //Default helpful methods
    default boolean isSubtype(TypeDef other) {
        if (this == other) return true;
        for (TypeDef supertype : typeCheckingSupertypes())
            if (supertype.isSubtype(other))
                return true;
        return false;
    }

    //Gets all methods, including the inherited ones.
    default List<MethodDef> getAllMethods() {
        TypeDef trueSupertype = inheritanceSupertype();
        //If there's no supertype, just return this object's methods
        if (trueSupertype == null)
            return methods();
        //If there is a supertype, grab its methods recursively
        List<? extends MethodDef> supertypeMethods = trueSupertype.getAllMethods();
        //Get my methods
        List<MethodDef> myMethods = new ArrayList<>(methods());

        //Add all non-duplicate methods from supertypeMethods into myMethods
        //Methods being duplicates or not is dependent on getSignature()
        Set<MethodDef.Signature> seenMethods = new HashSet<>(ListUtils.filter(ListUtils.map(myMethods, MethodDef::getSignature), Objects::nonNull));
        for (MethodDef def : supertypeMethods) {
            if (def.isStatic()) continue;
            if (def.isConstructor()) continue;
            MethodDef.Signature sig = def.getSignature();
            if (sig == null) continue;
            if (seenMethods.add(sig)) //if adding the signature is successful:
                myMethods.add(def);
        }
        //Return myMethods in the end
        return myMethods;
    }

}
