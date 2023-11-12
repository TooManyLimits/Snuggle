package ast.ir.def.method;

import ast.ir.def.CodeBlock;
import ast.typed.def.method.CustomMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import ast.typed.def.method.InterfaceMethodDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;

public interface GeneratedMethod {

    static GeneratedMethod of(MethodDef methodDef) throws CompilationException {
        if (methodDef instanceof SnuggleMethodDef snuggleMethodDef && snuggleMethodDef.numGenerics() == 0) {
            CodeBlock body = snuggleMethodDef.compileToCodeBlock();
            return new GeneratedSnuggleMethod(snuggleMethodDef, body);
        } else if (methodDef instanceof CustomMethodDef customMethodDef) {
            return new GeneratedBuiltinMethod(v -> customMethodDef.visitor().accept(v));
        } else if (methodDef instanceof InterfaceMethodDef interfaceMethodDef) {
            return new GeneratedInterfaceMethod(interfaceMethodDef);
        } else {
            return null;
        }
    }

    void compile(ClassVisitor classWriter) throws CompilationException;

}
