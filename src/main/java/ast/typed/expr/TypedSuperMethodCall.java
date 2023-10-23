package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import compile.BytecodeHelper;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public record TypedSuperMethodCall(Loc loc, Type receiverType, MethodDef method, List<TypedExpr> args, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        boolean isConstructorCall = method.name().equals("new");
        if (isConstructorCall) //Load "this" on the stack, if constructor
            visitor.visitVarInsn(Opcodes.ALOAD, 0);

        for (TypedExpr arg : args) //Push all args on the stack
            arg.compile(compiler, env, visitor);

        method.compileCall(Opcodes.INVOKESPECIAL, receiverType, compiler, visitor); //Invoke special

        if (isConstructorCall) //Push unit, if constructor
            BytecodeHelper.pushUnit(visitor);
    }
}
