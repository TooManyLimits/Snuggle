package ast.ir.def;

import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record GeneratedMethod(SnuggleMethodDef methodDef, CodeBlock body) {

    public static GeneratedMethod of(MethodDef methodDef) throws CompilationException {
        if (methodDef instanceof SnuggleMethodDef snuggleMethodDef && snuggleMethodDef.numGenerics() == 0) {
            CodeBlock body = snuggleMethodDef.compileToCodeBlock();
            return new GeneratedMethod(snuggleMethodDef, body);
        } else {
            return null;
        }
    }

    public void compile(ClassVisitor classWriter) throws CompilationException {
        int access = Opcodes.ACC_PUBLIC;
        if (methodDef.isStatic() || methodDef.owningType().isPlural()) access += Opcodes.ACC_STATIC;
        //Create writer
        MethodVisitor methodWriter = classWriter.visitMethod(access, methodDef.dedupName(), methodDef.getDescriptor(), null, null);
        //Visit params
        for (String s : methodDef.paramNames())
            methodWriter.visitParameter(s, 0);
        //Visit code...
        methodWriter.visitCode();
        body.writeJvmBytecode(methodWriter);
        methodWriter.visitMaxs(0, 0);
        methodWriter.visitEnd();
    }

}
