package ast.typed.def.method;

import ast.ir.def.CodeBlock;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.function.Consumer;

public record CustomMethodDef(String name, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType, CallCompiler callCompiler, Consumer<ClassVisitor> visitor) implements MethodDef {

    @Override
    public int numGenerics() {
        return 0;
    }

    @Override
    public void checkCode() throws CompilationException {
        //No code to check
    }

    @Override
    public void compileCall(boolean isSuperCall, CodeBlock block, List<FieldDef> desiredFields, MethodVisitor jvm) throws CompilationException {
        callCompiler.compileCall(isSuperCall, block, desiredFields, jvm);
    }

    @FunctionalInterface
    public interface CallCompiler {
        void compileCall(boolean isSuperCall, CodeBlock block, List<FieldDef> desiredFields, MethodVisitor jvm);
    }
}
