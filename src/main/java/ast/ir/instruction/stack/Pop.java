package ast.ir.instruction.stack;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.Instruction;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

//Pop the given object type from the stack
public record Pop(TypeDef type) implements Instruction {

    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) {
        if (type.isPlural()) {
            ListUtils.iterBackwards(type.fields(),
                    f -> new Pop(f.type()).accept(block, jvm)
            );
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
    public long cost() {
        //We trust JIT to combine multiple POP instructions into one.
        return type.stackSlots() == 0 ? 0 : 1; //Cost is marked as 1 because of this trust ^
    }
}
