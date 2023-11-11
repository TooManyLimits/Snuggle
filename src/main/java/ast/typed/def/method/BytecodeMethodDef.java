package ast.typed.def.method;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.misc.RunBytecode;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import util.LateInit;
import util.throwing_interfaces.ThrowingConsumer;

import java.util.List;

//leavesReturnOnStack:
//If this method returns a plural type, then this is true if the plural type
//is left on the stack, and false otherwise.
//If the method does not return a plural type, this boolean does not matter.
public record BytecodeMethodDef(String name, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType, boolean leavesReturnOnStack, BytecodeCall bytecode, LateInit<Long, CompilationException> cost, RunBytecode... argTransformers) implements MethodDef {

    public static final LateInit<Long, CompilationException> ONE = new LateInit<>(() -> 1L);
    public static final LateInit<Long, CompilationException> ZERO = new LateInit<>(() -> 0L);

    public BytecodeMethodDef(String name, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType, boolean leavesReturnOnStack, ThrowingConsumer<MethodVisitor, CompilationException> bytecode, LateInit<Long, CompilationException> cost, RunBytecode... argTransformers) {
        this(name, isStatic, owningType, paramTypes, returnType, leavesReturnOnStack, (b, d, v) -> bytecode.accept(v), cost);
    }

    public BytecodeMethodDef(String name, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType, boolean leavesReturnOnStack, ThrowingConsumer<MethodVisitor, CompilationException> bytecode, RunBytecode... argTransformers) {
        this(name, isStatic, owningType, paramTypes, returnType, leavesReturnOnStack, (b, d, v) -> bytecode.accept(v), ONE);
    }

    public BytecodeMethodDef(String name, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType, boolean leavesReturnOnStack, BytecodeCall bytecode, RunBytecode... argTransformers) {
        this(name, isStatic, owningType, paramTypes, returnType, leavesReturnOnStack, bytecode, ONE);
    }

    @Override
    public RunBytecode getArgumentTransformer(int index) {
        if (index >= argTransformers.length)
            return null;
        return argTransformers[index];
    }

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
    public void compileCall(boolean isSuperCall, CodeBlock block, List<FieldDef> desiredFields, MethodVisitor jvm) throws CompilationException {
        bytecode.accept(block, desiredFields, jvm);
    }

    @FunctionalInterface
    public interface BytecodeCall {
        void accept(CodeBlock block, List<FieldDef> desiredFields, MethodVisitor visitor) throws CompilationException;
    }

}
