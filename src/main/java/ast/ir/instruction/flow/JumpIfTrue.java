package ast.ir.instruction.flow;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.Instruction;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record JumpIfTrue(Label label) implements Instruction {

    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) {
        jvm.visitJumpInsn(Opcodes.IFNE, label);
    }

    @Override
    public long cost() {
        return 1;
    }
}
