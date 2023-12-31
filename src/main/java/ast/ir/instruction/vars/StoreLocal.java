package ast.ir.instruction.vars;

import ast.ir.def.CodeBlock;
import ast.ir.helper.BytecodeHelper;
import ast.ir.instruction.Instruction;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

//Store the value on top of the stack to the variable at the given index with the given type
public record StoreLocal(int index, TypeDef type) implements Instruction {

    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) {
        BytecodeHelper.visitVariable(index, type, true, jvm);
    }

    @Override
    public long cost() {
        return 1 + (type.stackSlots() - 1) / 2; //Costs more depending on stack slots
    }
}
