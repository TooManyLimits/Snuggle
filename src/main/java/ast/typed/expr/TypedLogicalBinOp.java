package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.flow.IrLabel;
import ast.ir.instruction.flow.JumpIfFalse;
import ast.ir.instruction.flow.JumpIfTrue;
import ast.ir.instruction.misc.InnerCodeBlock;
import ast.ir.instruction.stack.Dup;
import ast.ir.instruction.stack.Pop;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Label;

import java.util.Set;

public record TypedLogicalBinOp(Loc loc, boolean and, TypedExpr lhs, TypedExpr rhs, TypeDef type) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {
        lhs.findAllThisFieldAccesses(setToFill);
        rhs.findAllThisFieldAccesses(setToFill);
    }

    @Override
    public void compile(CodeBlock block, DesiredFieldNode desiredFields) throws CompilationException {
        Label end = new Label();
        CodeBlock rhsBlock = new CodeBlock(block);

        //Compile first part of lhs
        lhs.compile(block, null); //Compile lhs
        block.emit(new Dup(lhs.type())); //Dup it, since the jump will delete it
        block.emit(and ? new JumpIfFalse(end) : new JumpIfTrue(end)); //Jump to end if needed

        //Compile to rhs block
        rhsBlock.emit(new Pop(lhs.type())); //Pop the dup'ed value
        rhs.compile(rhsBlock, null); //Compile rhs
        //Emit the rhs block
        block.emit(new InnerCodeBlock(rhsBlock));

        //Emit end label
        block.emit(new IrLabel(end));
    }
}
