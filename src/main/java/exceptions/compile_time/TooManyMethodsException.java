package exceptions.compile_time;

import ast.typed.def.type.TypeDef;
import lexing.Loc;

public class TooManyMethodsException extends CompilationException {

    public final String methodName;

    public TooManyMethodsException(String methodName, Loc loc, TypeDef.InstantiationStackFrame cause) {
        super("Unable to determine which overload of \"" + methodName + "\" to call based on provided args and context" + (cause == null ? "." : ".\n" + cause.stackTrace()), loc);
        this.methodName = methodName;
    }
}
