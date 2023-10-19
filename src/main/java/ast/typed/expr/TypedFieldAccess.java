package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.field.FieldDef;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record TypedFieldAccess(Loc loc, TypedExpr lhs, FieldDef field, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        //Compile the LHS, pushing it on the stack
        lhs.compile(compiler, env, visitor);
        //Compile the GETFIELD
        field.compileAccess(Opcodes.GETFIELD, lhs.type(), compiler, visitor);
    }

    //Compile and prepare for a SETFIELD instruction soon
    public void compileForSet(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        //Compile LHS, pushing it on the stack
        lhs.compile(compiler, env, visitor);
        //And that's it! The other class will now compile the RHS, and then call
        //compileAccess() with the SETFIELD opcode.
    }

}
