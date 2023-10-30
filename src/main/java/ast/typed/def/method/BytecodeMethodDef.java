package ast.typed.def.method;

import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.function.Consumer;

public record BytecodeMethodDef(String name, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType, Consumer<MethodVisitor> bytecode) implements MethodDef {

    @Override
    public int numGenerics() {
        return 0;
    }

    @Override
    public TypeDef owningType() {
        return owningType;
    }

    @Override
    public void checkCode() throws CompilationException {
        //No code to check here
    }

    @Override
    public void compileCall(boolean isSuperCall, MethodVisitor jvm) {
        bytecode.accept(jvm);
    }
}
