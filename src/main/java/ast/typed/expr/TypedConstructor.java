package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.ArrayType;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public record TypedConstructor(Loc loc, Type type, MethodDef method, List<TypedExpr> args) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        TypeDef def = compiler.getTypeDef(type);
        if (def instanceof BuiltinTypeDef b && b.builtin() == ArrayType.INSTANCE) {
            //Arrays have special constructors
            for (TypedExpr arg : args)
                arg.compile(compiler, env, visitor);
            //Args don't matter for array constructor, except visitor
            method.compileCall(0, null, null, visitor);
        } else {
            //Otherwise, it's as usual
            visitor.visitTypeInsn(Opcodes.NEW, compiler.getTypeDef(type).getRuntimeName());
            visitor.visitInsn(Opcodes.DUP);
            for (TypedExpr arg : args)
                arg.compile(compiler, env, visitor);
            method.compileCall(Opcodes.INVOKESPECIAL, type, compiler, visitor);
        }
    }
}
