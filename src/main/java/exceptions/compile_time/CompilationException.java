package exceptions.compile_time;

import lexing.Loc;

public abstract class CompilationException extends Exception {

    public final Loc loc;

    public CompilationException(String message, Loc loc) {
        super(message + " at " + loc);
        this.loc = loc;
    }

}
