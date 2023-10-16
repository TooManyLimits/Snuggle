package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record TypedAssignment(Loc loc, TypedExpr lhs, TypedExpr rhs, Type type) implements TypedExpr {

    //Largely works similarly to TypedDeclaration.compile()
    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        //First compile the rhs, pushing its result on the stack
        rhs.compile(compiler, env, visitor);

        //Dup the rhs value
        if (compiler.getTypeDef(type) instanceof BuiltinTypeDef b) {
            if (b.builtin() instanceof IntegerType i && i.bits == 64)
                visitor.visitInsn(Opcodes.DUP2);
            else if (b.builtin() instanceof FloatType f && f.bits == 64)
                visitor.visitInsn(Opcodes.DUP2);
            else
                visitor.visitInsn(Opcodes.DUP);
        } else {
            visitor.visitInsn(Opcodes.DUP);
        }

        //Store
        if (lhs instanceof TypedVariable typedVariable) {
            int index = env.lookup(loc, typedVariable.name());
            TypeDef def = compiler.getTypeDef(type);
            TypedVariable.visitVariable(index, def, true, visitor);
        } else {
            throw new IllegalStateException("Attempting assignment to something other than var or field? Bug in compiler, please report!");
        }
    }
}
