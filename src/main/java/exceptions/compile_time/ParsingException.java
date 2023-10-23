package exceptions.compile_time;

import lexing.Loc;

public class ParsingException extends CompilationException {
    public ParsingException(String message, Loc loc) {
        super(message, loc);
    }
}
