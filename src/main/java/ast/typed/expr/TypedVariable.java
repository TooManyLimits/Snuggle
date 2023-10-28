package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.vars.LoadLocal;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record TypedVariable(Loc loc, String name, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock code) {
        int index = code.env.lookup(loc, name);
        code.emit(new LoadLocal(index, type.get()));
    }

}
