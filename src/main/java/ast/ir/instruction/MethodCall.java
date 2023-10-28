package ast.ir.instruction;

import ast.typed.def.method.MethodDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

//Call the given method. The receiver/args are on the stack already.
public record MethodCall(boolean isSuperCall, MethodDef methodToCall) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) {
        methodToCall.compileCall(isSuperCall, jvm);
    }

    @Override
    public int cost() {
        return 1; //Say it costs 1 to call
    }
}
