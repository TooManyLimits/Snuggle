package ast.typed.expr;

import ast.typed.Type;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public record TypedBlock(Loc loc, List<TypedExpr> exprs, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        env.push();
        for (int i = 0; i < exprs.size() - 1; i++)
            exprs.get(i).compileAndPop(compiler, env, visitor);
        exprs.get(exprs.size() - 1).compile(compiler, env, visitor);
        env.pop();
    }

}
