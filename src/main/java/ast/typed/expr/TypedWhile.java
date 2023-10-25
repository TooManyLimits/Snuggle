package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import compile.BytecodeHelper;
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
        Label condLabel = new Label();
        Label endLabel = new Label();

        //Get type def:
        Type innerType = ((BuiltinTypeDef) compiler.getTypeDef(type)).generics().get(0);
        TypeDef innerTypeDef = compiler.getTypeDef(innerType);

        //Start by pushing an Optional "None" on the stack.
        //This(these) stack slot(s) contain(s) the "current result".
        BytecodeHelper.pushNone(innerTypeDef, visitor);

        //Emit the cond label
        visitor.visitLabel(condLabel);
        //Compile the cond
        cond.compile(compiler, env, visitor);
        //If the cond was false, jump to end
        visitor.visitJumpInsn(Opcodes.IFEQ, endLabel);
        //Compile the inner expression, replacing the current result
        BytecodeHelper.pop(innerTypeDef, visitor); //pop the current result
        body.compile(compiler, env, visitor); //push the result of the body
        //Jump back to the top
        visitor.visitJumpInsn(Opcodes.GOTO, condLabel);
        //Finally emit end label
        visitor.visitLabel(endLabel);
        //Current result is on the stack.
    }
}
