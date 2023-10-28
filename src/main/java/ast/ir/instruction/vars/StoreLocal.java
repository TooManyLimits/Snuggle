package ast.ir.instruction.vars;

import ast.ir.helper.BytecodeHelper;
import ast.ir.instruction.Instruction;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

//Store the value on top of the stack to the variable at the given index with the given type
public record StoreLocal(int index, TypeDef type) implements Instruction {


    @Override
    public void accept(MethodVisitor jvm) {
        BytecodeHelper.visitVariable(index, type, true, jvm);
    }

    @Override
    public int cost() {
        return 1 + type().stackSlots() / 2; //Costs more depending on stack slots
    }
}
