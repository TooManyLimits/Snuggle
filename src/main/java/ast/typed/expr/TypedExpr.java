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
import util.MapStack;

public interface TypedExpr {

    //Still know the location
    Loc loc();

    //A TypedExpr should know its annotatedType once created.
    Type type();

    void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException;
    
    //Compiles and then pops the result off the stack
    default void compileAndPop(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        compile(compiler, env, visitor);
        TypeDef def = compiler.getTypeDef(type());

        //If output was a double or long, pop 2 slots
        if (def instanceof BuiltinTypeDef b) {
            if (b.builtin() instanceof IntegerType i) {
                if (i.bits == 64) {
                    visitor.visitInsn(Opcodes.POP2);
                    return;
                }
            } else if (b.builtin() instanceof FloatType f) {
                if (f.bits == 64) {
                    visitor.visitInsn(Opcodes.POP2);
                    return;
                }
            }
        }
        //Otherwise, just pop once
        visitor.visitInsn(Opcodes.POP);
    }



}
