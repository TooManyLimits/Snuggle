package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.flow.Return;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.ParsingException;
import lexing.Loc;

import java.util.Set;

public record TypedReturn(Loc loc, TypedExpr rhs, TypeDef type) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {
        rhs.findAllThisFieldAccesses(setToFill);
    }

    @Override
    public void compile(CodeBlock block, DesiredFieldNode desiredFields) throws CompilationException {
        if (block.methodDef == null)
            throw new IllegalStateException("Cannot return here - can only return inside a method. But this should have been caught earlier - bug in compiler, please report! loc = " + loc);
        rhs.compile(block, null);
        block.emit(new Return(block.methodDef, rhs.type()));
    }
}
