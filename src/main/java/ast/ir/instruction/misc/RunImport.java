package ast.ir.instruction.misc;

import ast.ir.helper.NameHelper;
import ast.ir.instruction.Instruction;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

//Run an import for the given file name
public record RunImport(String fileName) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) {
        //Get info
        String filesClass = NameHelper.getFilesClassName();
        String fieldName = NameHelper.getImportFieldName(fileName);
        String methodName = NameHelper.getImportMethodName(fileName);

        //If the import hasn't been called before, run the import, and set the field
        //to true indicating it's been called before.
        Label afterCall = new Label();
        jvm.visitFieldInsn(Opcodes.GETSTATIC, filesClass, fieldName, "Z");
        jvm.visitInsn(Opcodes.DUP); //dup the variable, we want to return it at the end
        jvm.visitJumpInsn(Opcodes.IFNE, afterCall);
        jvm.visitInsn(Opcodes.ICONST_1);
        jvm.visitFieldInsn(Opcodes.PUTSTATIC, filesClass, fieldName, "Z");
        jvm.visitMethodInsn(Opcodes.INVOKESTATIC, filesClass, methodName, "()V", false);
        jvm.visitLabel(afterCall);
    }

    @Override
    public int cost() {
        return 1;
    }
}
