package ast.ir.def.method;

import exceptions.compile_time.CompilationException;
import org.objectweb.asm.ClassVisitor;
import util.throwing_interfaces.ThrowingConsumer;

public record GeneratedBuiltinMethod(ThrowingConsumer<ClassVisitor, CompilationException> visitor) implements GeneratedMethod {

    @Override
    public void compile(ClassVisitor classWriter) throws CompilationException {
        visitor.accept(classWriter);
    }
}
