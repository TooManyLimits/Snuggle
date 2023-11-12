package ast.typed.def.method;

import ast.ir.def.CodeBlock;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

//A method def without an implementation
public record InterfaceMethodDef(String name, int numGenerics, boolean isStatic, TypeDef owningType, List<TypeDef> paramTypes, TypeDef returnType) implements MethodDef {

    @Override
    public void checkCode() throws CompilationException {
        //This has no code to check
    }

    @Override
    public void compileCall(boolean isSuperCall, CodeBlock block, List<FieldDef> desiredFields, MethodVisitor jvm) throws CompilationException {
        int instruction = Opcodes.INVOKEINTERFACE; //interface
        //Invoke the instruction
        jvm.visitMethodInsn(instruction, owningType.runtimeName(), name, getDescriptor(), true);
    }
}
