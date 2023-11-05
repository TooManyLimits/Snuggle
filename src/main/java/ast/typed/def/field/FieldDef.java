package ast.typed.def.field;

import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;

import java.util.Set;

public interface FieldDef {
    String name();
    TypeDef owningType();
    TypeDef type();
    boolean isStatic();
    default boolean pub() { return true; }

    //Check that the code (initializer) is correct
    void checkCode() throws CompilationException;

}
