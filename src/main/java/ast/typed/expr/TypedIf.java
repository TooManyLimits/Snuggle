package ast.typed.expr;

import ast.typed.Type;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record TypedIf(Loc loc, TypedExpr cond, TypedExpr ifTrue, TypedExpr ifFalse, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        //Compile condition, pushing a bool on the stack
        cond.compile(compiler, env, visitor);
        if (hasFalseBranch()) {
            //Generate labels
            Label ifFalseLabel = new Label();
            Label endLabel = new Label();
            //If the condition is false (0), jump to the "false" zone
            visitor.visitJumpInsn(Opcodes.IFEQ, ifFalseLabel);
            //Compile the "true" zone, leaving its result on the stack
            ifTrue.compile(compiler, env, visitor);
            //Jump to the end
            visitor.visitJumpInsn(Opcodes.GOTO, endLabel);
            //Emit the false zone label and compile it, leaving its result on the stack
            visitor.visitLabel(ifFalseLabel);
            ifFalse.compile(compiler, env, visitor);
            //Mark the end label
            visitor.visitLabel(endLabel);
        } else {
            throw new IllegalStateException("If expressions without else branches are not yet supported!");
        }
    }

    private boolean hasFalseBranch() {
        return ifFalse != null;
    }
}
