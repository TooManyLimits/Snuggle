package ast.ir.instruction.stack;

import ast.ir.instruction.Instruction;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

//Pop the given object type from the stack
public record Pop(TypeDef type) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) {
        if (type.isPlural()) {
            throw new IllegalStateException("Plural types not implemented yet");
        } else {
            if (type.stackSlots() == 1)
                jvm.visitInsn(Opcodes.POP);
            else if (type.stackSlots() == 2)
                jvm.visitInsn(Opcodes.POP2);
            else
                throw new IllegalStateException("Non-plural type with not 0 or 1 stack slots? Bug in compiler, please report!");
        }
    }

    @Override
    public int cost() {
        //We trust JIT to combine multiple POP instructions into one.
        return type.stackSlots() == 0 ? 0 : 1; //Cost is marked as 1 because of this trust ^
    }
}
