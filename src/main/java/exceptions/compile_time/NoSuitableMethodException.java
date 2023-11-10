package exceptions.compile_time;

import ast.typed.def.type.TypeDef;
import lexing.Loc;

public class NoSuitableMethodException extends CompilationException {

    public final String methodName;
    public final boolean isStatic;
    public final boolean isSuperCall;
    public final TypeDef receiverType;

    public NoSuitableMethodException(String methodName, boolean isStatic, boolean isSuperCall, TypeDef receiverType, Loc loc, TypeDef.InstantiationStackFrame cause) {
        super(getMessage(methodName, isStatic, isSuperCall, receiverType) + (cause == null ? "." : ".\n" + cause.stackTrace()), loc);
        this.methodName = methodName;
        this.isStatic = isStatic;
        this.isSuperCall = isSuperCall;
        this.receiverType = receiverType.get();
    }

    private static String getMessage(String methodName, boolean isStatic, boolean isSuperCall, TypeDef receiverType) {
        if (methodName.equals("new")) {
            if (isSuperCall)
                return "Unable to find suitable super() constructor for provided args";
            else
                return "Unable to find suitable constructor for provided args";
        } else
            return "Unable to find suitable " + (isStatic ? "static " : "") + "method \"" + methodName + "\" on type \"" + receiverType.name() + "\" with provided args";
    }
}
