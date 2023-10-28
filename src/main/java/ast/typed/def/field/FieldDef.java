package ast.typed.def.field;

import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;

public interface FieldDef {
    String name();
    TypeDef type();
    boolean isStatic();

    //Check that the code is correct
    void checkCode() throws CompilationException;

}
