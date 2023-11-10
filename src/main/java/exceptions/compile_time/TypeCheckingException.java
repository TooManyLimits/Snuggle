package exceptions.compile_time;

import ast.typed.def.type.TypeDef;
import lexing.Loc;

public class TypeCheckingException extends CompilationException {
    public TypeCheckingException(String message, Loc loc, TypeDef.InstantiationStackFrame cause) {
        super(message + (cause == null ? "." : ".\n" + cause.stackTrace()), loc);
    }

    public TypeCheckingException(TypeDef expectedType, String context, TypeDef actualType, Loc loc, TypeDef.InstantiationStackFrame cause) {
        super("Expected " + context + " to evaluate to \"" + expectedType + "\", but it actually was \"" + actualType + "\" at " + loc +
                (cause == null ? "." : ".\n" + cause.stackTrace()),
                loc
        );
    }
}
