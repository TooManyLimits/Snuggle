package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.TypeDef;
import compile.BytecodeHelper;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record TypedAssignment(Loc loc, TypedExpr lhs, TypedExpr rhs, Type type) implements TypedExpr {

    //Largely works similarly to TypedDeclaration.compile()
    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        //Store
        if (lhs instanceof TypedVariable variable) {
            //First compile the rhs, pushing its result on the stack
            rhs.compile(compiler, env, visitor);
            //Dup the rhs value
            BytecodeHelper.dup(compiler.getTypeDef(type), visitor, 0);
            //Set the variable
            int index = env.lookup(loc, variable.name());
            TypeDef def = compiler.getTypeDef(type);
            TypedVariable.visitVariable(index, def, true, visitor);
        } else if (lhs instanceof TypedFieldAccess fieldAccess) {
            //Prepare for set by compiling the lhs of the field access
            fieldAccess.compileForSet(compiler, env, visitor);
            //Compile our rhs
            rhs.compile(compiler, env, visitor);
            //Dup our value down past the lhs
            BytecodeHelper.dup(compiler.getTypeDef(type), visitor, 1);
            //put field
            fieldAccess.field().compileAccess(Opcodes.PUTFIELD, fieldAccess.lhs().type(), compiler, visitor);
        } else {
            throw new IllegalStateException("Attempting assignment to something other than var or field? Bug in compiler, please report!");
        }
    }
}
