package exceptions.compile_time;

import lexing.Loc;

public abstract class CompilationException extends Exception {

    public final Loc loc;

    public CompilationException(String message, Loc loc) {
        super(message + (message.endsWith(".") ? "" : ".") + "\nError occurred at " + loc);
        this.loc = loc;
    }

    public CompilationException(String message, Loc loc, Throwable cause) {
        super(message + " at " + loc, cause);
        this.loc = loc;
    }

}
