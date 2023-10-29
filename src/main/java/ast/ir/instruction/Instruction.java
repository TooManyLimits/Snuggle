package ast.ir.instruction;

import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

//An instruction
public interface Instruction {

    //Emit jvm bytecode for this operation
    void accept(MethodVisitor jvm) throws CompilationException;

    //Get the cost for this operation (not counting inner code blocks)
    long cost();

}
