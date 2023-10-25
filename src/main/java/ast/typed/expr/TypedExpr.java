package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import compile.BytecodeHelper;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
        BytecodeHelper.pop(def, visitor);
    }



}
