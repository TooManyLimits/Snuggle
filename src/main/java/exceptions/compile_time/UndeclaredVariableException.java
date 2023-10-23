package exceptions.compile_time;

import lexing.Loc;

public class UndeclaredVariableException extends CompilationException {
    public UndeclaredVariableException(String message, Loc loc) {
        super(message, loc);
    }
}
