package exceptions;

import lexing.Loc;

public class UnknownTypeException extends CompilationException {
    public UnknownTypeException(String message, Loc loc) {
        super(message, loc);
    }
}
