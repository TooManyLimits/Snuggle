package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.stack.Pop;
import ast.ir.instruction.stack.Push;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;
import java.util.Set;

public record TypedBlock(Loc loc, List<TypedExpr> exprs, TypeDef type) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {
        for (TypedExpr e : exprs)
            e.findAllThisFieldAccesses(setToFill);
    }

    @Override
    public void compile(CodeBlock code, DesiredFieldNode desired) throws CompilationException {
        code.env.push();
        for (int i = 0; i < exprs.size() - 1; i++) { //For all exprs but the last, compile and pop
            exprs.get(i).compile(code, null);
            code.emit(new Pop(exprs.get(i).type().get()));
        }
        exprs.get(exprs.size() - 1).compile(code, desired); //For last, don't pop, instead leave it on stack
        code.env.pop();
    }

}
