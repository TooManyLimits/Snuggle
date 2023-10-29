package ast.ir.instruction.objects;

import ast.ir.instruction.Instruction;
import ast.typed.def.field.FieldDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

//Get from a field (reference type version). The receiver and value are on the stack
public record SetReferenceTypeField(FieldDef field) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) throws CompilationException {
        int opcode = field.isStatic() ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
        String owner = field.owningType().runtimeName();
        List<String> descriptor = field.type().getDescriptor();
        if (descriptor.size() > 1)
            throw new IllegalStateException("Trying to get plural field with SetReferenceTypeField instruction? Bug in compiler, please report!");
        jvm.visitFieldInsn(opcode, owner, field.name(), descriptor.get(0));
    }

    @Override
    public long cost() {
        return 1;
    }
}
