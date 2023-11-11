package ast.ir.instruction.stack;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.Instruction;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record Dup(TypeDef typeDef) implements Instruction {
    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) throws CompilationException {
        if (typeDef.isPlural())
            throw new IllegalStateException("Plural topLevelTypes not yet implemented");
        else if (typeDef.stackSlots() == 1)
            jvm.visitInsn(Opcodes.DUP);
        else if (typeDef.stackSlots() == 2)
            jvm.visitInsn(Opcodes.DUP2);
        else
            throw new IllegalStateException("Non-plural type with not 0 or 1 stack slots? Bug in compiler, please report!");
    }

    @Override
    public long cost() {
        return 0;
    }
}
