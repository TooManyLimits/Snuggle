package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.types.BoolType;
import builtin_types.types.UnitType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record TypedVariable(Loc loc, String name, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        int index = env.lookup(loc, name);
        TypeDef def = compiler.getTypeDef(type);
        visitVariable(index, def, false, visitor);
    }

    public static void visitVariable(int index, TypeDef def, boolean store, MethodVisitor visitor) {
        if (def instanceof BuiltinTypeDef b) {
            if (b.builtin() instanceof IntegerType i) {
                switch (i.bits) {
                    case 8, 16, 32 -> visitor.visitVarInsn(store ? Opcodes.ISTORE : Opcodes.ILOAD, index);
                    case 64 -> visitor.visitVarInsn(store ? Opcodes.LSTORE : Opcodes.LLOAD, index);
                    default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
                }
            } else if (b.builtin() instanceof FloatType f) {
                switch (f.bits) {
                    case 32 -> visitor.visitVarInsn(store ? Opcodes.FSTORE : Opcodes.FLOAD, index);
                    case 64 -> visitor.visitVarInsn(store ? Opcodes.DSTORE : Opcodes.DLOAD, index);
                    default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
                }
            } else if (b.builtin() == BoolType.INSTANCE) {
                visitor.visitVarInsn(store ? Opcodes.ISTORE : Opcodes.ILOAD, index);
            } else {
                visitor.visitVarInsn(store ? Opcodes.ASTORE :Opcodes.ALOAD, index);
            }
        } else {
            //For now, non-builtins are all reference types
            visitor.visitVarInsn(store ? Opcodes.ASTORE :Opcodes.ALOAD, index);
        }
    }


}
