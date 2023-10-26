package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import compile.BytecodeHelper;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
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
    private void castNumericNumeric(MethodVisitor visitor, TypeDef lhsTypeDef, BuiltinTypeDef b) throws CompilationException {
        if (lhsTypeDef instanceof BuiltinTypeDef b2 && b2.isNumeric()) {
            BuiltinType lhsBuiltin = b2.builtin();
            BuiltinType myTypeBuiltin = b.builtin();
            //Int -> Int
            if (lhsBuiltin instanceof IntegerType from && myTypeBuiltin instanceof IntegerType to) {
                if (from.bits <= 32 && to.bits <= 32) {
                    //Stuff 32 bits and under, can work without long/int converting on stack
                    if (from.bits < to.bits) {
                        //Smaller -> larger
                        //Most are no-op, except:
                        if (from.signed && !to.signed) {
                            //Signed -> Unsigned
                            //bitwise and with input size
                            switch (from.bits) {
                                case 8 -> visitor.visitIntInsn(Opcodes.SIPUSH, 0xff);
                                case 16 -> visitor.visitLdcInsn(0xffff);
                                default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                            }
                            visitor.visitInsn(Opcodes.IAND);
                        }
                    } else if (from.bits == to.bits) {
                        if (from.bits == 32) //32 bit -> 32 bit, nothing to be done, no-op
                            if (from.signed == to.signed)
                                throw new IllegalStateException("Invalid cast - Bits are same, and signs are same? Bug in compiler, please report!");
                            else
                                return;

                        if (from.signed && !to.signed) {
                            //bitwise and with input size
                            switch (from.bits) {
                                case 8 -> visitor.visitIntInsn(Opcodes.SIPUSH, 0xff);
                                case 16 -> visitor.visitLdcInsn(0xffff);
                                default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                            }
                            visitor.visitInsn(Opcodes.IAND);
                        } else if (!from.signed && to.signed) {
                            //sign extend
                            switch (to.bits) {
                                case 8 -> visitor.visitInsn(Opcodes.I2B);
                                case 16 -> visitor.visitInsn(Opcodes.I2S);
                                default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                            }
                        } else {
                            throw new IllegalStateException("Invalid cast - Bits are same, and signs are same? Bug in compiler, please report!");
                        }
                    } else {
                        //Larger -> smaller
                        //Signed -> signed is a no-op, others are not
                        if (!from.signed || !to.signed) {
                            //Bitwise and with output size
                            switch (to.bits) {
                                case 8 -> visitor.visitIntInsn(Opcodes.SIPUSH, 0xff);
                                case 16 -> visitor.visitLdcInsn(0xffff);
                                default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                            }
                            visitor.visitInsn(Opcodes.IAND);
                            if (to.signed) {
                                //Need to sign-extend
                                switch (to.bits) {
                                    case 8 -> visitor.visitInsn(Opcodes.I2B);
                                    case 16 -> visitor.visitInsn(Opcodes.I2S);
                                    default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                                }
                            }
                        }
                    }
                }
                //One or two 64-bit integer types are involved
                else if (from.bits == 64 && to.bits == 64) { //Get this case out of the way
                    if (from.signed == to.signed)
                        throw new IllegalStateException("Invalid cast - Bits are same, and signs are same? Bug in compiler, please report!");
                    //No-op, nothing to be done converting between two 64-bit types. Just reports a compiler bug if
                    //necessary.
                } else if (to.bits == 64) {
                    //Smaller type -> 64 bits
                    visitor.visitInsn(Opcodes.I2L);
                    //Most conversions are no-op, except:
                    if (!from.signed && to.signed) { // u8/u16/u32 -> i64: Need to mask value
                        visitor.visitLdcInsn(switch (from.bits) {
                            case 8 -> 0xffL;
                            case 16 -> 0xffffL;
                            case 32 -> 0xffffffffL;
                            default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                        });
                        visitor.visitInsn(Opcodes.LAND);
                    }
                } else if (from.bits == 64) {
                    //64 bit -> smaller type
                    visitor.visitInsn(Opcodes.L2I);
                    if (!to.signed && to.bits != 32) {
                        //If output is unsigned and <32 bit, then need to mask
                        switch (to.bits) {
                            case 8 -> visitor.visitIntInsn(Opcodes.SIPUSH, 0xff);
                            case 16 -> visitor.visitLdcInsn(0xffff);
                            default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                        }
                        visitor.visitInsn(Opcodes.IAND);
                    } else if (!from.signed && to.bits != 32) {
                        //u64 -> i8/i16/i32. Need to clip short and then sign-extend
                        switch (to.bits) {
                            case 8 -> visitor.visitInsn(Opcodes.I2B);
                            case 16 -> visitor.visitInsn(Opcodes.I2S);
                            default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                        }
                    }
                    //Signed -> signed is a no-op
                } else {
                    throw new IllegalStateException("Invalid bit counts " + from.bits + ", " + to.bits);
                }
                return;
            }
            //Int -> Float
            if (lhsBuiltin instanceof IntegerType from && myTypeBuiltin instanceof FloatType to) {
                if (from.signed)
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
                else
                    switch (from.bits) {
                        case 8, 16 -> {switch (to.bits) {
                            case 32 -> visitor.visitInsn(Opcodes.I2F);
                            case 64 -> visitor.visitInsn(Opcodes.I2D);
                            default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                        }}
                        case 32 -> {
                            //u32 -> f32/f64
                            visitor.visitInsn(Opcodes.I2L);
                            visitor.visitLdcInsn(0xffffffffL);
                            visitor.visitInsn(Opcodes.LAND);
                            visitor.visitInsn(to.bits == 32 ? Opcodes.L2F : Opcodes.L2D);
                        }
                        case 64 -> {switch (to.bits) {
                            //u64 -> f32/f64
                            //Implementations below are based on:
                            /*
                                double dValue = (double) (value & 0x7fffffffffffffffL);
                                if (value < 0) {
                                    dValue += 0x1.0p63;
                                }
                                // From https://stackoverflow.com/questions/24193788/convert-unsigned-64-bit-decimal-to-java-double
                                // Modified to work for other types as well
                             */
                            case 32 -> {
                                //start: [long]
                                visitor.visitInsn(Opcodes.DUP2); //[long, long]
                                visitor.visitLdcInsn(0x7fffffffffffffffL); //[long, long, mask]
                                visitor.visitInsn(Opcodes.LAND); //[long, masked long]
                                visitor.visitInsn(Opcodes.L2F); //[long, float]
                                BytecodeHelper.swapBigSmall(visitor); //[float, long]
                                Label ifNonNegative = new Label();
                                visitor.visitInsn(Opcodes.LCONST_0); //[float, long, 0L]
                                visitor.visitInsn(Opcodes.LCMP); //[float, int]
                                visitor.visitJumpInsn(Opcodes.IFGE, ifNonNegative); //jump if int non-negative. [float]
                                visitor.visitLdcInsn(0x1.0p63f); //2^63 as a float. [float, 0x1.0p63f]
                                visitor.visitInsn(Opcodes.FADD); //[float]
                                visitor.visitLabel(ifNonNegative); //non-negative, ends with [float]
                            }
                            case 64 -> {
                                //start: [long]
                                visitor.visitInsn(Opcodes.DUP2); //[long, long]
                                visitor.visitLdcInsn(0x7fffffffffffffffL); //[long, long, mask]
                                visitor.visitInsn(Opcodes.LAND); //[long, masked long]
                                visitor.visitInsn(Opcodes.L2D); //[long, double]
                                BytecodeHelper.swapBigBig(visitor); //[double, long]
                                Label ifNonNegative = new Label();
                                visitor.visitInsn(Opcodes.LCONST_0); //[double, long, 0L]
                                visitor.visitInsn(Opcodes.LCMP); //[double, int]
                                visitor.visitJumpInsn(Opcodes.IFGE, ifNonNegative); //jump if int non-negative. [double]
                                visitor.visitLdcInsn(0x1.0p63d); //2^63 as a double. [double, 0x1.0p63d]
                                visitor.visitInsn(Opcodes.DADD); //add [double]
                                visitor.visitLabel(ifNonNegative); //non-negative, [double]
                            }
                            default -> throw new IllegalStateException("Invalid bit count " + to.bits);
                        }}
                        default -> throw new IllegalStateException("Invalid bit count " + from.bits);
                    }
                return;
            }
            //Float -> Int
            if (lhsBuiltin instanceof FloatType from && myTypeBuiltin instanceof IntegerType to) {
                if (to.signed) {
                    switch (to.bits) {
                        case 8 -> {
                            visitor.visitInsn(from.bits == 32 ? Opcodes.F2I : Opcodes.D2I);
                            visitor.visitInsn(Opcodes.I2B);
                        }
                        case 16 -> {
                            visitor.visitInsn(from.bits == 32 ? Opcodes.F2I : Opcodes.D2I);
                            visitor.visitInsn(Opcodes.I2S);
                        }
                        case 32 -> visitor.visitInsn(from.bits == 32 ? Opcodes.F2I : Opcodes.D2I);
                        case 64 -> visitor.visitInsn(from.bits == 32 ? Opcodes.F2L : Opcodes.D2L);
                    }
                } else {
                    if (to.bits <= 32) {
                        visitor.visitInsn(from.bits == 32 ? Opcodes.F2L : Opcodes.D2L);
                        visitor.visitInsn(Opcodes.L2I);
                        if (to.bits < 32) {
                            if (to.bits == 8)
                                visitor.visitIntInsn(Opcodes.SIPUSH, 0xff); //push u8-max
                            else
                                visitor.visitLdcInsn(0xffff); //push u16-max
                            visitor.visitInsn(Opcodes.IAND);
                        }
                    } else { //f32 -> u64 or f64 -> u64
                        throw new TypeCheckingException("Casting float to unsigned int is not yet supported", loc);
                    }
                }
                return;
            }
            //Float -> Float
            if (lhsBuiltin instanceof FloatType from && myTypeBuiltin instanceof FloatType to) {
                if (from.bits > to.bits)
                    visitor.visitInsn(Opcodes.D2F);
                else if (to.bits > from.bits)
                    visitor.visitInsn(Opcodes.F2D);
                else
                    throw new IllegalStateException("Invalid cast - Bits are same, and signs are same? Bug in compiler, please report!");
                return;
            }
            throw new IllegalStateException("Casting number to number, but types are not numeric? Bug in compiler, please report!");
        }
        throw new IllegalStateException("TypedCast type is numeric but lhs is not numeric? Bug in compiler, please report!");
    }
}
