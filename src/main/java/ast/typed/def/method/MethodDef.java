package ast.typed.def.method;

import ast.passes.TypeChecker;
import ast.passes.TypePool;
import ast.typed.expr.TypedStaticMethodCall;
import compile.Compiler;
import exceptions.compile_time.CompilationException;
import ast.typed.Type;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedMethodCall;
import org.objectweb.asm.MethodVisitor;
import util.ListUtils;

import java.util.List;

public interface MethodDef {

    String name();
    int numGenerics();
    boolean isStatic();
    List<Type> paramTypes(); //Some may be Type.Generic, if numGenerics > 0
    Type returnType(); //May be Type.Generic, if numGenerics > 0

    //Const method handling
    boolean isConst();
    TypedExpr doConst(TypedMethodCall typedCall) throws CompilationException;
    TypedExpr doConstStatic(TypedStaticMethodCall typedCall) throws CompilationException;

    //Compile a call to this method
    void compileCall(int opcode, Type owner, Compiler compiler, MethodVisitor visitor) throws CompilationException;

    //Compare specificity of this method's args with another method's args.
    //If this one is more specific, return a negative value.
    //If neither is more specific, return 0.
    //If the other one is more specific, return a positive value.
    //The result is that sort()-ing this list will put the most specific method(s) at the front.
    default int compareSpecificity(MethodDef other, TypeChecker checker) {
        TypePool pool = checker.pool();
        int currentState = 0;
        List<Type> myParams = paramTypes();
        List<Type> otherParams = other.paramTypes();

        for (int i = 0; i < myParams.size(); i++) {
            Type myParam = myParams.get(i);
            Type otherParam = otherParams.get(i);

            if (i == 0) {
                if (myParam.isSubtype(otherParam, pool)) {
                    if (otherParam.isSubtype(myParam, pool))
                        currentState = currentState; //same type, don't change currentState
                    else
                        currentState = -1;
                } else {
                    if (otherParam.isSubtype(myParam, pool))
                        currentState = 1;
                    else
                        currentState = currentState;
                }
            } else {
                if (myParam.isSubtype(otherParam, pool)) {
                    if (otherParam.isSubtype(myParam, pool))
                        currentState = currentState; //same type, don't change currentState
                    else
                        if (currentState != -1)
                            return 0;
                } else {
                    if (otherParam.isSubtype(myParam, pool))
                        if (currentState != 1)
                            return 0;
                    else
                            currentState = currentState;
                }
            }
        }

        if (currentState == 0) {
            //Presumably all the args are the same?
            //TODO: Remove once we're sure this assertion holds
            if (!myParams.equals(otherParams))
                return 0;

            //Now choose the most specific return type
            if (returnType().isSubtype(other.returnType(), pool)) {
                if (other.returnType().isSubtype(returnType(), pool))
                    throw new IllegalStateException("Two methods with same signature? Bug in compiler, please report!");
                else
                    return -1;
            } else {
                if (other.returnType().isSubtype(returnType(), pool))
                    return 1;
                else
                    return 0;
            }
        }

        return currentState;
    }

    /**
     * Generate an int list deterministically from the signature.
     * Two methods with the same signature will have the same output.
     *
     * If any param or the return type is generic: returns null.
     * Caller should handle this case.
     *
     * This is only used for determining method overriding; a method
     * with the same Signature as one in the parent class will be
     * considered overriding it.
     */
    default Signature getSignature() {
        List<Type> paramTypes = paramTypes();
        Type returnType = returnType();
        for (Type paramType : paramTypes)
            if (paramType instanceof Type.Generic)
                return null;
        if (returnType instanceof Type.Generic)
            return null;
        return new Signature(
                name(),
                ListUtils.map(paramTypes, p -> ((Type.Basic) p).index()),
                ((Type.Basic) returnType).index()
        );
    }

    //Literally just an opaque data type used above to figure out method overriding
    record Signature(String name, List<Integer> params, int returnType) {}

}
