package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record TypedCast(Loc loc, int tokenLine, TypedExpr lhs, boolean isMaybe, Type type) implements TypedExpr {

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        //Load lhs on the stack first
        lhs.compile(compiler, env, visitor);

        //Emit line number
        Label label = new Label();
        visitor.visitLabel(label);
        visitor.visitLineNumber(loc.startLine(), label);

        //Get some info
        TypeDef myTypeDef = compiler.getTypeDef(type);
        TypeDef lhsTypeDef = compiler.getTypeDef(lhs.type());

        //Numeric -> Numeric case
        if (myTypeDef instanceof BuiltinTypeDef b && b.isNumeric()) {
            castNumericNumeric(visitor, lhsTypeDef, b);
        } else {
            if (isMaybe) {
                throw new IllegalStateException("Option type not yet implemented!");
            } else {
                visitor.visitTypeInsn(Opcodes.CHECKCAST, myTypeDef.getRuntimeName());
            }
        }

    }

    //Numeric -> Numeric logic, extracted to a new method because it's long
    private void castNumericNumeric(MethodVisitor visitor, TypeDef lhsTypeDef, BuiltinTypeDef b) {
        if (lhsTypeDef instanceof BuiltinTypeDef b2 && b2.isNumeric()) {
            BuiltinType lhsBuiltin = b2.builtin();
            BuiltinType myTypeBuiltin = b.builtin();
            //Int -> Int
            if (lhsBuiltin instanceof IntegerType from && myTypeBuiltin instanceof IntegerType to) {
                switch (from.bits) {
                    case 8 -> {switch (to.bits) {
                        case 8, 16, 32 -> {}
                        case 64 -> visitor.visitInsn(Opcodes.I2L);
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    case 16 -> {switch (to.bits) {
                        case 8 -> visitor.visitInsn(Opcodes.I2B);
                        case 16, 32 -> {}
                        case 64 -> visitor.visitInsn(Opcodes.I2L);
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    case 32 -> {switch (to.bits) {
                        case 8 -> visitor.visitInsn(Opcodes.I2B);
                        case 16 -> visitor.visitInsn(Opcodes.I2S);
                        case 32 -> {}
                        case 64 -> visitor.visitInsn(Opcodes.I2L);
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    case 64 -> {switch (to.bits) {
                        case 8 -> {
                            visitor.visitInsn(Opcodes.L2I);
                            visitor.visitInsn(Opcodes.I2B);
                        }
                        case 16 -> {
                            visitor.visitInsn(Opcodes.L2I);
                            visitor.visitInsn(Opcodes.I2S);
                        }
                        case 32 -> visitor.visitInsn(Opcodes.L2I);
                        case 64 -> visitor.visitInsn(Opcodes.I2L);
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                }
                return;
            }
            //Int -> Float
            if (lhsBuiltin instanceof IntegerType from && myTypeBuiltin instanceof FloatType to) {
                switch (from.bits) {
                    case 8, 16, 32 -> {switch (to.bits) {
                        case 32 -> visitor.visitInsn(Opcodes.I2F);
                        case 64 -> visitor.visitInsn(Opcodes.I2D);
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    case 64 -> {switch (to.bits) {
                        case 32 -> visitor.visitInsn(Opcodes.L2F);
                        case 64 -> visitor.visitInsn(Opcodes.L2D);
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                }
                return;
            }
            //Float -> Int
            if (lhsBuiltin instanceof FloatType from && myTypeBuiltin instanceof IntegerType to) {
                switch (from.bits) {
                    case 32 -> {switch (to.bits) {
                        case 8 -> {
                            visitor.visitInsn(Opcodes.F2I);
                            visitor.visitInsn(Opcodes.I2B);
                        }
                        case 16 -> {
                            visitor.visitInsn(Opcodes.F2I);
                            visitor.visitInsn(Opcodes.I2S);
                        }
                        case 32 -> visitor.visitInsn(Opcodes.F2I);
                        case 64 -> visitor.visitInsn(Opcodes.F2L);
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    case 64 -> {switch (to.bits) {
                        case 8 -> {
                            visitor.visitInsn(Opcodes.D2I);
                            visitor.visitInsn(Opcodes.I2B);
                        }
                        case 16 -> {
                            visitor.visitInsn(Opcodes.D2I);
                            visitor.visitInsn(Opcodes.I2S);
                        }
                        case 32 -> visitor.visitInsn(Opcodes.D2I);
                        case 64 -> visitor.visitInsn(Opcodes.D2L);
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                }
                return;
            }
            //Float -> Float
            if (lhsBuiltin instanceof FloatType from && myTypeBuiltin instanceof FloatType to) {
                switch (from.bits) {
                    case 32 -> {switch (to.bits) {
                        case 32 -> {}
                        case 64 -> visitor.visitInsn(Opcodes.F2D);
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    case 64 -> {switch (to.bits) {
                        case 32 -> visitor.visitInsn(Opcodes.D2F);
                        case 64 -> {}
                        default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                    }}
                    default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                }
                return;
            }
            throw new IllegalStateException("Casting number to number, but types are not numeric? Bug in compiler, please report!");
        }
        throw new IllegalStateException("TypedCast type is numeric but lhs is not numeric? Bug in compiler, please report!");
    }
}
