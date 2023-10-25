package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.ArrayType;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public record TypedConstructor(Loc loc, Type type, MethodDef method, List<TypedExpr> args) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        TypeDef def = compiler.getTypeDef(type);

        //Visit the line number:
        Label label = new Label();
        visitor.visitLabel(label);
        visitor.visitLineNumber(loc.startLine(), label);

        if (def instanceof BuiltinTypeDef b && b.hasSpecialConstructor()) {
            //Some types have special constructors
            for (TypedExpr arg : args)
                arg.compile(compiler, env, visitor);
            //Args shouldn't matter for special constructor, except the visitor
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
