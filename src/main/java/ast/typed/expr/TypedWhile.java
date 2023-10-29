package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.misc.InnerCodeBlock;
import ast.ir.instruction.flow.IrLabel;
import ast.ir.instruction.flow.Jump;
import ast.ir.instruction.flow.JumpIfFalse;
import ast.ir.instruction.stack.Pop;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import lexing.Loc;
import org.objectweb.asm.Label;
import util.ListUtils;

import java.util.List;

public record TypedWhile(Loc loc, TypedExpr cond, TypedExpr body, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock code) {
        Label condLabel = new Label();
        Label endLabel = new Label();

        //Push optional "none" as the current "result" :iea:
        MethodDef constructorDef = ListUtils.find(type.methods(), method -> method.name().equals("new") && method.paramTypes().size() == 0);
        if (constructorDef == null)
            throw new IllegalStateException("Options of non-reference types not yet implemented!");
        new TypedConstructor(loc, type, constructorDef, List.of()).compile(code);

        code.emit(new IrLabel(condLabel)); //Emit cond label

        //Compile everything that happens multiple times into their own CodeBlocks
        CodeBlock condBlock = new CodeBlock(code);
        CodeBlock bodyBlock = new CodeBlock(code);

        cond.compile(condBlock); //Compile cond into the cond block
        condBlock.emit(new JumpIfFalse(endLabel)); //Compile jumping to the end (inside the cond block)
        code.emit(new InnerCodeBlock(condBlock)); //Emit the cond block

        bodyBlock.emit(new Pop(type)); //Pop the "result"
        body.compile(bodyBlock); //Compile body into the body block
        bodyBlock.emit(new Jump(condLabel)); //Jump to the start (inside the body block)
        code.emit(new InnerCodeBlock(bodyBlock)); //Emit the body block

        code.emit(new IrLabel(endLabel)); //End label
    }
}
