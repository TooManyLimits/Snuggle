package ast.ir.instruction.objects;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.Instruction;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import ast.ir.helper.BytecodeHelper;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

//If isMaybe, "to" is the *inner* type of the Option, not
//the Option itself.
public record Cast(TypeDef from, TypeDef to, boolean isMaybe) implements Instruction {

    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) {
        TypeDef from = from().get();
        TypeDef to = to().get();
        //[from] is on the stack.
        if (to.isNumeric())
            castNumericNumeric(jvm); //[from] -> [to]
        else if (from.isReferenceType() && to.isReferenceType()) {
            if (isMaybe) {
                performMaybeCast(jvm);
            } else
                jvm.visitTypeInsn(Opcodes.CHECKCAST, to.name()); //[from] -> [to]
        } else {
            throw new IllegalStateException("Unrecognized casting operation? Bug in compiler, please report!");
        }
    }

    @Override
    public long cost() {
        //Any casts are interpreted as costing 1, even though some casts are more expensive than others
        //(particularly those involving unsigned ints)
        return 1;
    }

    //from and to are both reference topLevelTypes. Stack goes [from] -> [to?]
    private void performMaybeCast(MethodVisitor jvm) {
        jvm.visitInsn(Opcodes.DUP); //dup value [from, from]
        jvm.visitTypeInsn(Opcodes.INSTANCEOF, to.name()); //instanceof [from, bool]
        //If the bool is false, then pop and push null. If it's true, do nothing.
        Label ifTrue = new Label();
        jvm.visitJumpInsn(Opcodes.IFNE, ifTrue);
        jvm.visitInsn(Opcodes.POP);
        jvm.visitInsn(Opcodes.ACONST_NULL);
        jvm.visitLabel(ifTrue);
    }

    //Casting between numeric topLevelTypes. Separated to its own method because it's big.
    //[from] -> [to]
    private void castNumericNumeric(MethodVisitor visitor) {
        if (from.isNumeric()) {
            BuiltinType lhsBuiltin = from.builtin();
            BuiltinType myTypeBuiltin = to.builtin();
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
                //One or two 64-bit integer topLevelTypes are involved
                else if (from.bits == 64 && to.bits == 64) { //Get this case out of the way
                    if (from.signed == to.signed)
                        throw new IllegalStateException("Invalid cast - Bits are same, and signs are same? Bug in compiler, please report!");
                    //No-op, nothing to be done converting between two 64-bit topLevelTypes. Just reports a compiler bug if
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
                                // Modified to work for other topLevelTypes as well
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
//                        throw new TypeCheckingException("Casting float to unsigned int is not yet supported", loc);
                        throw new IllegalStateException("Casting float to unsigned int is not yet supported");
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
            throw new IllegalStateException("Casting number to number, but topLevelTypes are not numeric? Bug in compiler, please report!");
        }
        throw new IllegalStateException("TypedCast type is numeric but lhs is not numeric? Bug in compiler, please report!");
    }

}
