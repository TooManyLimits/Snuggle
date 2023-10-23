package ast.typed.expr;

import ast.typed.Type;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import runtime.Unit;

public record TypedWhile(Loc loc, TypedExpr cond, TypedExpr body, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        //TODO: Make this actually return something other than unit, once Option is working
        Label condLabel = new Label();
        Label endLabel = new Label();

        //Emit the cond label
        visitor.visitLabel(condLabel);
        //Compile the cond
        cond.compile(compiler, env, visitor);
        //If the cond was false, jump to end
        visitor.visitJumpInsn(Opcodes.IFEQ, endLabel);
        //Compile the inner expression (and pop it TODO: Don't pop it, instead keep it for result value)
        body.compileAndPop(compiler, env, visitor);
        //Jump back to the top
        visitor.visitJumpInsn(Opcodes.GOTO, condLabel);
        //Finally emit end label
        visitor.visitLabel(endLabel);

        //TODO: Remove
        //push unit as the result (temporary)
        String unitName = org.objectweb.asm.Type.getInternalName(Unit.class);
        visitor.visitFieldInsn(Opcodes.GETSTATIC, unitName, "INSTANCE", "L" + unitName + ";");

    }
}
