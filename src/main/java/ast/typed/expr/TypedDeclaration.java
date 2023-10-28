package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.vars.LoadLocal;
import ast.ir.instruction.vars.StoreLocal;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record TypedDeclaration(Loc loc, String name, TypeDef type, TypedExpr rhs) implements TypedExpr {

    @Override
    public void compile(CodeBlock code) {
        rhs.compile(code); //First compile the rhs, pushing its result on the stack
        int index = code.env.declare(loc, name, type); //Get the index
        code.emit(new StoreLocal(index, type.get())); //Store
        code.emit(new LoadLocal(index, type.get())); //Then load
    }
}
