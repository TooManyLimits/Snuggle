package ast.typed.def.method;

import ast.ir.def.CodeBlock;
import ast.ir.helper.ScopeHelper;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.GenericTypeDef;
import ast.typed.def.type.StructDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedStaticMethodCall;
import exceptions.compile_time.CompilationException;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedMethodCall;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public interface MethodDef {

    //Information that a MethodDef *has*.

    String name();
    int numGenerics();
    boolean isStatic();
    List<TypeDef> paramTypes(); //Some may be GenericTypeDef, if numGenerics > 0
    TypeDef returnType(); //May be GenericTypeDef, if numGenerics > 0
    TypeDef owningType(); //The type that this method is on

    default boolean pub() { return true; }; //MethodDef other than SnuggleMethodDef should always be pub

    //Things that a MethodDef can *do*.

    void checkCode() throws CompilationException;

    //Compile a call to this method. The receiver (if applicable) and the arguments are on the stack.
    //block is rarely used, except for core builtin types.
    //desiredFields indicates, for builtin functions that return plural types, and leave their output on the stack:
    //it indicates which fields should actually be put onto the stack. Also only used in core builtin types.
    void compileCall(boolean isSuperCall, CodeBlock block, List<FieldDef> desiredFields, MethodVisitor jvm);

    //Constant-fold the method. By default, does nothing and just returns the input.
    default TypedExpr constantFold(TypedMethodCall call) { return call; }
    default TypedExpr constantFold(TypedStaticMethodCall call) { return call; }
    default MethodDef delegate() { return this; }

    default boolean isConstructor() { return name().equals("new"); }

    default String getDescriptor() {
        String returnTypeDescriptor = isConstructor() ? "V" : returnType().getReturnTypeDescriptor();

        StringBuilder b = new StringBuilder("(");
        if (owningType().get() instanceof StructDef structDef && !isConstructor())
            for (String s : structDef.getDescriptor())
                b.append(s);
        for (TypeDef p : paramTypes())
            for (String s : p.getDescriptor())
                b.append(s);
        return b.append(")").append(returnTypeDescriptor).toString();
    }

    //Compare specificity of this method's args with another method's args.
    //If this one is more specific, return a negative value.
    //If neither is more specific, return 0.
    //If the other one is more specific, return a positive value.
    //The result is that sort()-ing this list will put the most specific method(s) at the front.
    default int compareSpecificity(MethodDef other) {
        int currentState = 0;
        List<TypeDef> myParams = paramTypes();
        List<TypeDef> otherParams = other.paramTypes();

        for (int i = 0; i < myParams.size(); i++) {
            TypeDef myParam = myParams.get(i);
            TypeDef otherParam = otherParams.get(i);

            if (i == 0) {
                if (myParam.isSubtype(otherParam)) {
                    if (otherParam.isSubtype(myParam))
                        currentState = currentState; //same type, don't change currentState
                    else
                        currentState = -1;
                } else {
                    if (otherParam.isSubtype(myParam))
                        currentState = 1;
                    else
                        currentState = currentState;
                }
            } else {
                if (myParam.isSubtype(otherParam)) {
                    if (otherParam.isSubtype(myParam))
                        currentState = currentState; //same type, don't change currentState
                    else
                        if (currentState != -1)
                            return 0;
                } else {
                    if (otherParam.isSubtype(myParam))
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
            if (returnType().isSubtype(other.returnType())) {
                if (other.returnType().isSubtype(returnType()))
                    throw new IllegalStateException("Two methods with same signature? Bug in compiler, please report!");
                else
                    return -1;
            } else {
                if (other.returnType().isSubtype(returnType()))
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
        List<TypeDef> paramTypes = paramTypes();
        TypeDef returnType = returnType();
        for (TypeDef paramType : paramTypes)
            if (paramType instanceof GenericTypeDef)
                return null;
        if (returnType instanceof GenericTypeDef)
            return null;
        return new Signature(name(), paramTypes, returnType);
    }

    //Literally just a data type used above to figure out method overriding
    record Signature(String name, List<TypeDef> params, TypeDef returnType) {}

}
