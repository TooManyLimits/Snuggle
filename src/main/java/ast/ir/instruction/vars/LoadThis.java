package ast.ir.instruction.vars;

import ast.ir.instruction.Instruction;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

//Load "this".
//Is only used in a super-call context, meaning
//the type is assumed to be a reference type, and we're
//calling a regular non-static method (not a struct receiver)
public record LoadThis(TypeDef type) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) {
        jvm.visitVarInsn(Opcodes.ALOAD, 0);
    }

    @Override
    public long cost() {
        return 1; //Loading this costs 1
    }
}
