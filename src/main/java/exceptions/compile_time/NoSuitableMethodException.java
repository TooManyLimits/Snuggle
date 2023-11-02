package exceptions.compile_time;

import ast.typed.def.type.TypeDef;
import lexing.Loc;

public class NoSuitableMethodException extends CompilationException {

    public final String methodName;
    public final boolean isSuperCall;
    public final TypeDef receiverType;

    public NoSuitableMethodException(String methodName, boolean isSuperCall, TypeDef receiverType, Loc loc) {
        super(getMessage(methodName, isSuperCall, receiverType), loc);
        this.methodName = methodName;
        this.isSuperCall = isSuperCall;
        this.receiverType = receiverType.get();
    }

    private static String getMessage(String methodName, boolean isSuperCall, TypeDef receiverType) {
        if (methodName.equals("new")) {
            if (isSuperCall)
                return "Unable to find suitable super() constructor for provided args";
            else
                return "Unable to find suitable constructor for provided args";
        } else
            return "Unable to find suitable method \"" + methodName + "\" on type \"" + receiverType.name() + "\" with provided args";
    }
}
