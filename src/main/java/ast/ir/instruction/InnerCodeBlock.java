package ast.ir.instruction;

import ast.ir.def.CodeBlock;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

public record InnerCodeBlock(CodeBlock codeBlock) implements Instruction {
    @Override
    public void accept(MethodVisitor jvm) throws CompilationException {
        //Write the inner codeBlock
        codeBlock.writeJvmBytecode(jvm);
    }

    @Override
    public int cost() {
        return 0; //Cost is handled by the inner codeblock
    }
}
