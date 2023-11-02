package exceptions.compile_time;

import lexing.Loc;

public class TooManyMethodsException extends CompilationException {

    public final String methodName;

    public TooManyMethodsException(String methodName, Loc loc) {
        super("Unable to determine which overload of \"" + methodName + "\" to call based on provided args and context", loc);
        this.methodName = methodName;
    }
}
