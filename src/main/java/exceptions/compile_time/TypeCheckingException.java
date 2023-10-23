package exceptions.compile_time;

import lexing.Loc;

public class TypeCheckingException extends CompilationException {
    public TypeCheckingException(String message, Loc loc) {
        super(message, loc);
    }
}
