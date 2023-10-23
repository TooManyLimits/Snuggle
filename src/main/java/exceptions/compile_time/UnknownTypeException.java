package exceptions.compile_time;

import lexing.Loc;

public class UnknownTypeException extends CompilationException {
    public UnknownTypeException(String message, Loc loc) {
        super(message, loc);
    }
}
