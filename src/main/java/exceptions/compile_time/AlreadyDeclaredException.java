package exceptions.compile_time;

import lexing.Loc;

public class AlreadyDeclaredException extends CompilationException {
    public AlreadyDeclaredException(String message, Loc loc) {
        super(message, loc);
    }
}
