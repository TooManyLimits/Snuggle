package ast.typed.def.field;

import ast.typed.Type;
import ast.typed.expr.TypedExpr;
import compile.Compiler;
import compile.NameHelper;
import compile.ScopeHelper;
import exceptions.CompilationException;
import lexing.Loc;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.LateInit;

//initializer may be null
public record SnuggleFieldDef(Loc loc, boolean pub, boolean isStatic, String name, Type type, LateInit<TypedExpr, CompilationException> initializer) implements FieldDef {

    public void compile(Type thisType, Compiler compiler, ClassWriter classWriter) throws CompilationException {
        FieldVisitor writer = classWriter.visitField(Opcodes.ACC_PUBLIC, getGeneratedName(), getDescriptor(compiler), null, null);
        writer.visitEnd();
    }

    //This takes place inside a constructor
    @Override
    public boolean compileInit(Type thisType, Compiler compiler, MethodVisitor visitor) throws CompilationException {
        if (initializer != null) {
            //Push this on the stack
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            //Compile the initializer
            ScopeHelper scope = new ScopeHelper();
            if (!isStatic) //non-static methods have "this" as their first local
                scope.declare(loc, compiler, "this", thisType);
            initializer.get().compile(compiler, scope, visitor);
            //Assign result into this
            compileAccess(Opcodes.PUTFIELD, thisType, compiler, visitor);
            return true;
        }
        return false;
    }

    public String getGeneratedName() { return NameHelper.getFieldName(name, type); }
    public String getDescriptor(Compiler compiler) { return compiler.getTypeDef(type).getDescriptor(); }

    @Override
    public void compileAccess(int opcode, Type owner, Compiler compiler, MethodVisitor visitor) {
        visitor.visitFieldInsn(opcode, compiler.getTypeDef(owner).getRuntimeName(), getGeneratedName(), getDescriptor(compiler));
    }

}
