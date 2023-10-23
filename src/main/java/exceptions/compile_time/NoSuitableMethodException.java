package exceptions.compile_time;

import lexing.Loc;

public class NoSuitableMethodException extends CompilationException {
    public NoSuitableMethodException(String message, Loc loc) {
        super(message, loc);
    }
}
