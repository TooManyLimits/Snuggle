package ast.typed.def.field;

import ast.typed.Type;
import compile.Compiler;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;

public interface FieldDef {
    String name();
    Type type();
    boolean isStatic();
    void compileAccess(int opcode, Type owner, Compiler compiler, MethodVisitor visitor);

    //If the init was compiled (it had an initializer) return true.
    //If it wasn't, return false.
    boolean compileInit(Type thisType, Compiler compiler, MethodVisitor visitor) throws CompilationException;
}
