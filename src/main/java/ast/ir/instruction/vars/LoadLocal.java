package ast.ir.instruction.vars;

import ast.ir.def.CodeBlock;
import ast.ir.helper.BytecodeHelper;
import ast.ir.instruction.Instruction;
import ast.typed.def.type.TypeDef;
import org.objectweb.asm.MethodVisitor;

//Load the local at the given index with the given type
public record LoadLocal(int index, TypeDef type) implements Instruction {

    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) {
        BytecodeHelper.visitVariable(index, type, false, jvm);
    }

    @Override
    public long cost() {
        //Higher cost depending on stack slots used
        return 1 + (type.stackSlots() - 1) / 2;
    }
}
