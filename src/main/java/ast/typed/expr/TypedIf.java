package ast.typed.expr;

import ast.type_resolved.TypeCheckingHelper;
import ast.typed.Type;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * At this stage of the AST, a TypedIf always has an ifFalse branch.
 * If it didn't have one at the previous stage of the AST, it was filled
 * in with an empty Option constructor.
 */
public record TypedIf(Loc loc, TypedExpr cond, TypedExpr ifTrue, TypedExpr ifFalse, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        //Compile condition, pushing a bool on the stack
        cond.compile(compiler, env, visitor);
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
    }
}
