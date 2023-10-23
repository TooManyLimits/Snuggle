package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.TypeDef;
import compile.BytecodeHelper;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;

public record TypedDeclaration(Loc loc, String name, Type type, TypedExpr rhs) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        //First compile the rhs, pushing its result on the stack
        rhs.compile(compiler, env, visitor);

        //Dup the result value
        BytecodeHelper.dup(compiler.getTypeDef(type), visitor, 0);

        //Store
        int index = env.declare(loc, compiler, name, type);
        TypeDef def = compiler.getTypeDef(type);
        TypedVariable.visitVariable(index, def, true, visitor);
    }
}
