package ast.typed.def.type;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.*;

public interface TypeDef {
    String name();

    // Attempt to convert this type to a storable type.
    // For example, StringLiteral -> String.
    // Or IntLiteral -> Int.
    // However, generally for IntLiteral we cannot convert directly
    // to storable, because it's unknown which storable type we want.
    // A compiler flag may be added in the future which causes the default
    // int type to be i32, and default float type to be f64, like Java.
    Type toStorable(Type thisType, Loc loc, TypePool pool) throws CompilationException;

    boolean isSubtype(Type other, TypePool pool);

    //The "true" supertype of this type, which methods can be inherited from
    Type trueSupertype() throws CompilationException;

    List<? extends MethodDef> getMethods() throws CompilationException;
    List<? extends FieldDef> getFields() throws CompilationException;

    //Check the bodies of methods as well as the initializers of fields
    void checkCode() throws CompilationException;

    //The descriptor for this type
    String getDescriptor();

    //The name at runtime for this type
    String getRuntimeName();

    //Is this type extensible?
    boolean extensible();

    //Gets all methods, including the true supertype's.
    default List<? extends MethodDef> getAllMethods(TypePool pool) throws CompilationException {
        Type trueSupertype = trueSupertype();
        //If there's no supertype, just return this object's methods
        if (trueSupertype == null)
            return getMethods();
        //If there is a supertype, grab its methods recursively
        List<? extends MethodDef> supertypeMethods = pool.getTypeDef(trueSupertype).getAllMethods(pool);
        //Get my methods
        List<MethodDef> myMethods = new ArrayList<>(getMethods());

        //Add all non-duplicate methods from supertypeMethods into myMethods
        //Methods being duplicates or not is dependent on getSignature()
        Set<MethodDef.Signature> seenMethods = new HashSet<>(ListUtils.filter(ListUtils.map(myMethods, MethodDef::getSignature), Objects::nonNull));
        for (MethodDef def : supertypeMethods) {
            if (def.isStatic()) continue;
            if (def instanceof SnuggleMethodDef snuggleDef && snuggleDef.isConstructor()) continue;
            MethodDef.Signature sig = def.getSignature();
            if (sig == null) continue;
            if (seenMethods.add(sig)) //if adding the signature is successful:
                myMethods.add(def);
        }
        //Return myMethods in the end
        return myMethods;
    }

}
