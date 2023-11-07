package ast.ir.instruction.stack;

import ast.ir.def.CodeBlock;
import ast.ir.helper.BytecodeHelper;
import ast.ir.instruction.Instruction;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

//Stores the top element in block.env.maxIndex()
//Used in conjunction with other things like SetField and GetField
public record StoreInTopLocal(TypeDef typeDef) implements Instruction {
    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) throws CompilationException {
        //Now store
        BytecodeHelper.visitVariable(block.env.maxIndex(), typeDef, true, jvm);
    }

    @Override
    public long cost() {
        return 1;
    }
}
