package ast.typed.def.method;

import ast.ir.def.CodeBlock;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//leavesReturnOnStack:
//If this method returns a plural type, then this is true if the plural type
//is left on the stack, and false otherwise.
//If the method does not return a plural type, this boolean does not matter.
public record BytecodeMethodDef(String name, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType, boolean leavesReturnOnStack, BytecodeCall bytecode) implements MethodDef {

    public BytecodeMethodDef(String name, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType, boolean leavesReturnOnStack, Consumer<MethodVisitor> bytecode) {
        this(name, isStatic, owningType, paramTypes, returnType, leavesReturnOnStack, (b, d, v) -> bytecode.accept(v));
    }

//    public BytecodeMethodDef(String name, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType, boolean leavesReturnOnStack, BiConsumer<CodeBlock, MethodVisitor> bytecode) {
//        this(name, isStatic, owningType, paramTypes, returnType, leavesReturnOnStack, (b, d, v) -> bytecode.accept(v));
//    }

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
    public void compileCall(boolean isSuperCall, CodeBlock block, List<FieldDef> desiredFields, MethodVisitor jvm) {
        bytecode.accept(block, desiredFields, jvm);
    }

    @FunctionalInterface
    public interface BytecodeCall {
        void accept(CodeBlock block, List<FieldDef> desiredFields, MethodVisitor visitor);
    }

}
