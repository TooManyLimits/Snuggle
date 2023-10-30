package ast.ir.instruction.misc;

import ast.ir.instruction.Instruction;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

//Emit a line number
public record LineNumber(int line) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) {
        //Visit the line number:
        Label label = new Label();
        jvm.visitLabel(label);
        jvm.visitLineNumber(line, label);
    }

    @Override
    public long cost() {
        return 0; //No cost to this of course
    }
}
