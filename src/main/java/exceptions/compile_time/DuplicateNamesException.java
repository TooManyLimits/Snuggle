package exceptions.compile_time;

import lexing.Loc;

public class DuplicateNamesException extends CompilationException {
    public DuplicateNamesException(String message, Loc loc) {
        super(message, loc);
    }
}
