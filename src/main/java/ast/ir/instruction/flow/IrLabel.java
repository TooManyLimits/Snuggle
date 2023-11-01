package ast.ir.instruction.flow;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.Instruction;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

//Named to avoid conflict with ASM Label class
public record IrLabel(Label asmLabel) implements Instruction {
    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) {
        jvm.visitLabel(asmLabel);
    }

    @Override
    public long cost() {
        return 0;
    }
}
