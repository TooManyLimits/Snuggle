package ast.typed.def.field;

import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.LateInit;

public record SnuggleFieldDef(Loc loc, boolean pub, String name, TypeDef owningType, TypeDef type, boolean isStatic, LateInit<TypedExpr, CompilationException> initializer) implements FieldDef {

    @Override
    public void checkCode() throws CompilationException {
        if (initializer != null)
            initializer.get();
    }
}
