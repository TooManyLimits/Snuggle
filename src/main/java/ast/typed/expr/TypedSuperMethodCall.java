package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import compile.BytecodeHelper;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public record TypedSuperMethodCall(Loc loc, Type receiverType, MethodDef method, List<TypedExpr> args, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        visitor.visitVarInsn(Opcodes.ALOAD, 0); //Load "this" on the stack
        for (TypedExpr arg : args)
            arg.compile(compiler, env, visitor); //Push all args on the stack
        method.compileCall(Opcodes.INVOKESPECIAL, receiverType, compiler, visitor); //Invoke special
        if (method.name().equals("new"))
            BytecodeHelper.pushUnit(visitor);
    }
}
