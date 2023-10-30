package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.misc.InnerCodeBlock;
import ast.ir.instruction.flow.IrLabel;
import ast.ir.instruction.flow.Jump;
import ast.ir.instruction.flow.JumpIfFalse;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Label;

/**
 * At this stage of the AST, a TypedIf always has an ifFalse branch.
 * If it didn't have one at the previous stage of the AST, it was filled
 * in with an empty Option constructor.
 */
public record TypedIf(Loc loc, TypedExpr cond, TypedExpr ifTrue, TypedExpr ifFalse, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock code, DesiredFieldNode desiredFields) throws CompilationException {

        Label ifFalseLabel = new Label();
        Label endLabel = new Label();

        CodeBlock trueBlock = new CodeBlock(code);
        CodeBlock falseBlock = new CodeBlock(code);

        cond.compile(code, null); //Push cond on the stack
        code.emit(new JumpIfFalse(ifFalseLabel)); //Jump if false

        ifTrue.compile(trueBlock, desiredFields); //Compile ifTrue (into the true block)
        trueBlock.emit(new Jump(endLabel)); //Jump to end label (inside the true block)
        code.emit(new InnerCodeBlock(trueBlock));

        code.emit(new IrLabel(ifFalseLabel)); //Begin false branch
        ifFalse.compile(falseBlock, desiredFields); //Compile ifFalse (into the false block)
        code.emit(new InnerCodeBlock(falseBlock));

        code.emit(new IrLabel(endLabel)); //End
    }
}
