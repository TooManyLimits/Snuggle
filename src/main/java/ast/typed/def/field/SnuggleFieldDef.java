package ast.typed.def.field;

import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record SnuggleFieldDef(Loc loc, boolean pub, String name, TypeDef type, boolean isStatic) implements FieldDef {

    @Override
    public void checkCode() throws CompilationException {
        //No code to check, yet
    }
}
