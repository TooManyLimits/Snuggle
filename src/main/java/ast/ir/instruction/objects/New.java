package ast.ir.instruction.objects;

import ast.ir.instruction.Instruction;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

//Pushes a new instance of the class on the stack, and also DUPs it.
//Assumes typeDef is a reference type.
public record New(TypeDef typeDef) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) {
        jvm.visitTypeInsn(Opcodes.NEW, typeDef.runtimeName());
        jvm.visitInsn(Opcodes.DUP);
        //Assumed that constructor will be called right after this.
    }

    @Override
    public long cost() {
        return 0; //Calling the constructor will be the cost.
    }
}
