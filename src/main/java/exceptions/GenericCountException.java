package exceptions;

import lexing.Loc;

public class GenericCountException extends CompilationException {
    public GenericCountException(String message, Loc loc) {
        super(message, loc);
    }
}
