package ast.ir.def.method;

import ast.ir.def.CodeBlock;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.GenericStringUtil;

public record GeneratedSnuggleMethod(SnuggleMethodDef methodDef, CodeBlock body) implements GeneratedMethod {

    public void compile(ClassVisitor classWriter) throws CompilationException {
        int access = Opcodes.ACC_PUBLIC;
        if (methodDef.isStatic() || methodDef.owningType().isPlural()) access += Opcodes.ACC_STATIC;
        //Create writer
        MethodVisitor methodWriter = classWriter.visitMethod(access, GenericStringUtil.mangleSlashes(methodDef.dedupName()), methodDef.getDescriptor(), null, null);
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
