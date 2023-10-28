package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public interface TypedExpr {

    //Still know the location
    Loc loc();

    //A TypedExpr should know its TypeDef once created.
    TypeDef type();

    void compile(CodeBlock block);

    //Compiles and then pops the result off the stack
//    default void compileAndPop(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
//        compile(compiler, env, visitor);
//        TypeDef def = compiler.getTypeDef(type());
//        BytecodeHelper.pop(def, visitor);
//    }



}
