package exceptions.compile_time;

import lexing.Loc;

public class TooManyMethodsException extends CompilationException {
    public TooManyMethodsException(String message, Loc loc) {
        super(message, loc);
    }
}
