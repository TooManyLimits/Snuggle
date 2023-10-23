package exceptions.runtime;

/**
 * This differs from the other exceptions in the fact that it
 * occurs when running Snuggle code, not when compiling it.
 * Hence, it is not a CompilationException.
 */
public class SnuggleException extends Exception {

    public SnuggleException(String message) {
        super(message);
    }

}
