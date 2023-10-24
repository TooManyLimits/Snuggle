package ast.typed.expr;

import ast.typed.Type;
import compile.Compiler;
import compile.NameHelper;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record TypedImport(Loc loc, String fileName, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {

        //Visit the line number:
        Label label = new Label();
        visitor.visitLabel(label);
        visitor.visitLineNumber(loc.startLine(), label);

        //Get info
        String filesClass = NameHelper.getFilesClassName(compiler.instanceId);
        int fileId = compiler.fileIndicesByName.get(fileName);
        String fieldName = NameHelper.getImportFieldName(fileId);
        String methodName = NameHelper.getImportMethodName(fileId, fileName);

        //If the import hasn't been called before, run the import, and set the field
        //to true indicating it's been called before.
        Label afterCall = new Label();
        visitor.visitFieldInsn(Opcodes.GETSTATIC, filesClass, fieldName, "Z");
        visitor.visitInsn(Opcodes.DUP); //dup the variable, we want to return it at the end
        visitor.visitJumpInsn(Opcodes.IFNE, afterCall);
        visitor.visitInsn(Opcodes.ICONST_1);
        visitor.visitFieldInsn(Opcodes.PUTSTATIC, filesClass, fieldName, "Z");
        visitor.visitMethodInsn(Opcodes.INVOKESTATIC, filesClass, methodName, "()V", false);
        visitor.visitLabel(afterCall);
    }
}
