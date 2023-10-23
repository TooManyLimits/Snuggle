package ast.typed.def.method;

import ast.typed.Type;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedMethodCall;
import ast.typed.expr.TypedStaticMethodCall;
import builtin_types.types.BoolType;
import builtin_types.types.UnitType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import compile.Compiler;
import compile.NameHelper;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.LateInit;

import java.util.List;

public record SnuggleMethodDef(Loc loc, boolean isStatic, String name, int numGenerics, List<String> paramNames, List<Type> paramTypes, Type returnType, LateInit<TypedExpr, CompilationException> body) implements MethodDef {
    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public TypedExpr doConst(TypedMethodCall typedCall) {
        return null;
    }

    @Override
    public TypedExpr doConstStatic(TypedStaticMethodCall typedCall) {
        return null;
    }

    public void compile(Type thisType, Compiler compiler, ClassWriter classWriter) throws CompilationException {

        if (numGenerics != 0) {
            throw new IllegalStateException("No method generics yet");
        }

        MethodVisitor writer = classWriter.visitMethod(Opcodes.ACC_PUBLIC, getGeneratedName(), getDescriptor(compiler), null, null);
        for (String s : paramNames)
            writer.visitParameter(s, 0);
        writer.visitCode();

        //Handle special constructor calls
        if (isConstructor()) {
            //Initialize fields
            List<? extends FieldDef> fields = compiler.getTypeDef(thisType).getFields();
            for (FieldDef field : fields)
                field.compileInit(thisType, compiler, writer);
        }

        ScopeHelper scope = new ScopeHelper();
        if (!isStatic) //non-static methods have "this" as their first local
            scope.declare(loc, compiler, "this", thisType);
        for (int i = 0; i < paramNames.size(); i++)
            scope.declare(loc, compiler, paramNames.get(i), paramTypes.get(i));
        body.get().compile(compiler, scope, writer);

        if (compiler.getTypeDef(returnType) instanceof BuiltinTypeDef b) {
            if (b.builtin() == UnitType.INSTANCE) {
                if (isConstructor()) {
                    //Constructors can't return any value in the jvm
                    writer.visitInsn(Opcodes.POP);
                    writer.visitInsn(Opcodes.RETURN);
                } else {
                    writer.visitInsn(Opcodes.ARETURN);
                }
            } else if (b.builtin() instanceof IntegerType i) {
                if (i.bits <= 32)
                    writer.visitInsn(Opcodes.IRETURN);
                else
                    writer.visitInsn(Opcodes.LRETURN);
            } else if (b.builtin() instanceof FloatType f) {
                if (f.bits == 32)
                    writer.visitInsn(Opcodes.FRETURN);
                else
                    writer.visitInsn(Opcodes.DRETURN);
            } else if (b.builtin() == BoolType.INSTANCE)
                writer.visitInsn(Opcodes.IRETURN);
            else
                writer.visitInsn(Opcodes.ARETURN);
        } else {
            writer.visitInsn(Opcodes.ARETURN);
        }

        writer.visitMaxs(0, 0); //Auto compute
        writer.visitEnd();
    }

    //Get the generated methodName of this method.
    public String getGeneratedName() {
        //java hard-forces constructors to have the methodName "<init>"
        if (isConstructor())
            return "<init>";
        return NameHelper.getMethodName(name, paramTypes, returnType);
    }

    //Get descriptor
    public String getDescriptor(Compiler compiler) {
        StringBuilder descriptor = new StringBuilder("(");
        for (Type t : paramTypes)
            descriptor.append(compiler.getTypeDef(t).getDescriptor());
        descriptor.append(")");
        if (isConstructor())
            descriptor.append("V"); //Constructors are forced to have *actual* void returns, not faked ones with Unit
        else
            descriptor.append(compiler.getTypeDef(returnType).getDescriptor());
        return descriptor.toString();
    }

    public boolean isConstructor() {
        return name.equals("new"); //No other SnuggleMethod can have this name, as it's a keyword
    }

    @Override
    public void compileCall(int opcode, Type owner, Compiler compiler, MethodVisitor visitor) throws CompilationException {
        visitor.visitMethodInsn(opcode, compiler.getTypeDef(owner).getRuntimeName(), getGeneratedName(), getDescriptor(compiler), false);
    }
}
