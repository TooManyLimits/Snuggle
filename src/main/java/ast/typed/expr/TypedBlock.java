package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.stack.Pop;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;

public record TypedBlock(Loc loc, List<TypedExpr> exprs, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock code) {
        code.env.push();
        for (int i = 0; i < exprs.size() - 1; i++) { //For all exprs but the last, compile and pop
            exprs.get(i).compile(code);
            code.emit(new Pop(exprs.get(i).type().get()));
        }
        exprs.get(exprs.size() - 1).compile(code); //For last, don't pop, instead leave it on stack
        code.env.pop();
    }

}
