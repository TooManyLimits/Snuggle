package ast.ir.def.method;

import ast.typed.def.method.InterfaceMethodDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.GenericStringUtil;

public record GeneratedInterfaceMethod(InterfaceMethodDef methodDef) implements GeneratedMethod {

    @Override
    public void compile(ClassVisitor classWriter) throws CompilationException {
        int access = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT;
        MethodVisitor v = classWriter.visitMethod(access, GenericStringUtil.mangleSlashes(methodDef.name()), methodDef.getDescriptor(), null, null);
        v.visitEnd();
    }
}
