package ast.ir.instruction.misc;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.Instruction;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

import java.util.function.BiConsumer;

//Literally just runs some bytecode
public record RunBytecode(long cost, BiConsumer<CodeBlock, MethodVisitor> visitor) implements Instruction {

    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) throws CompilationException {
        visitor.accept(block, jvm);
    }
}
