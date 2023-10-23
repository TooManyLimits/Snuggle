package builtin_types.types.numbers;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.ConstMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedMethodCall;
import builtin_types.BuiltinType;
import builtin_types.helpers.DefineConstWithFallback;
import builtin_types.types.BoolType;
import compile.BytecodeHelper;
import exceptions.CompilationException;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.IntLiteralData;
import util.ListUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class IntegerType implements BuiltinType {

    /**
     *
     * Definitions of all integer types!
     *
     */

    public final boolean signed;
    public final int bits;
    public final BigInteger min, max;
    public final String name;
    public final String descriptor;

    private IntegerType(boolean signed, int bits) {
        this.signed = signed;
        this.bits = bits;
        this.name = (signed ? "i" : "u") + bits;
        //Calculate min and max
        this.min = signed ? BigInteger.ONE.shiftLeft(bits-1).negate() : BigInteger.ZERO;
        this.max = BigInteger.ONE.shiftLeft(signed ? bits-1 : bits).subtract(BigInteger.ONE);
        this.descriptor = switch (bits) {
            case 8 -> "B";
            case 16 -> "S";
            case 32 -> "I";
            case 64 -> "J";
            default -> throw new IllegalStateException("Illegal bit count for int literal data??? bits = " + bits);
        };
    }

    public static BuiltinType fromIntData(IntLiteralData data) {
        if (!data.isSpecified())
            return IntLiteralType.INSTANCE;
        return switch (data.bits()) {
            case 8 -> data.signed() ? I8 : U8;
            case 16 -> data.signed() ? I16 : U16;
            case 32 -> data.signed() ? I32 : U32;
            case 64 -> data.signed() ? I64 : U64;
            default -> throw new IllegalStateException("Illegal bit count for int literal data? bits = " + data.bits());
        };
    }

    public boolean fits(BigInteger value) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    public static final IntegerType I8 = new IntegerType(true, 8);
    public static final IntegerType I16 = new IntegerType(true, 16);
    public static final IntegerType I32 = new IntegerType(true, 32);
    public static final IntegerType I64 = new IntegerType(true, 64);
    public static final IntegerType U8 = new IntegerType(false, 8);
    public static final IntegerType U16 = new IntegerType(false, 16);
    public static final IntegerType U32 = new IntegerType(false, 32);
    public static final IntegerType U64 = new IntegerType(false, 64);

    public static final List<IntegerType> ALL_INT_TYPES = List.of(I8, I16, I32, I64, U8, U16, U32, U64);

    /**
     *
     * Methods of integer types!
     *
     */

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<? extends MethodDef> getMethods(List<Type> generics, TypePool pool) throws CompilationException {
        Type type = pool.getBasicBuiltin(this);
        Type boolType = pool.getBasicBuiltin(BoolType.INSTANCE);

        return ListUtils.join(List.of(
                //Regular binary operators
                DefineConstWithFallback.defineBinary("add", BigInteger::add, type, type, doOperationThenConvert(v -> v.visitInsn(bits <= 32 ? Opcodes.IADD : Opcodes.LADD))),
                DefineConstWithFallback.defineBinary("sub", BigInteger::subtract, type, type, doOperationThenConvert(v -> v.visitInsn(bits <= 32 ? Opcodes.ISUB : Opcodes.LSUB))),
                DefineConstWithFallback.defineBinary("mul", BigInteger::multiply, type, type, doOperationThenConvert(v -> v.visitInsn(bits <= 32 ? Opcodes.IMUL : Opcodes.LMUL))),
                //IntelliJ is bugged, so it thinks these are errors; really they compile fine
                DefineConstWithFallback.defineBinary("div", BigInteger::divide, type, type, doOperationThenConvert(switch (bits) {
                    case 8, 16 -> v -> v.visitInsn(Opcodes.IDIV);
                    case 32 -> signed ? v -> v.visitInsn(Opcodes.IDIV) :
                            v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "divideUnsigned", "(II)I", false);
                    case 64 -> signed ? v -> v.visitInsn(Opcodes.LDIV) :
                            v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "divideUnsigned", "(JJ)J", false);
                    default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
                })),
                DefineConstWithFallback.defineBinary("rem", BigInteger::remainder, type, type, doOperationThenConvert(switch (bits) {
                    case 8, 16 -> v -> v.visitInsn(Opcodes.IREM);
                    case 32 -> signed ? v -> v.visitInsn(Opcodes.IREM) :
                            v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "remainderUnsigned", "(II)I", false);
                    case 64 -> signed ? v -> v.visitInsn(Opcodes.LREM) :
                            v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "remainderUnsigned", "(JJ)J", false);
                    default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
                })),

                //Bitwise binary
                DefineConstWithFallback.defineBinary("band", BigInteger::and, type, type, doOperationThenConvert(v -> v.visitInsn(bits <= 32 ? Opcodes.IAND : Opcodes.LAND))),
                DefineConstWithFallback.defineBinary("bor", BigInteger::or, type, type, doOperationThenConvert(v -> v.visitInsn(bits <= 32 ? Opcodes.IOR : Opcodes.LOR))),
                DefineConstWithFallback.defineBinary("bxor", BigInteger::xor, type, type, doOperationThenConvert(v -> v.visitInsn(bits <= 32 ? Opcodes.IXOR : Opcodes.LXOR))),

                //Unary
                signed ? DefineConstWithFallback.defineUnary("neg", BigInteger::negate, type, doOperationThenConvert(v -> v.visitInsn(bits <= 32 ? Opcodes.INEG : Opcodes.LNEG))) : List.of(),
                DefineConstWithFallback.defineUnary("bnot", BigInteger::not, type, doOperationThenConvert(switch (bits) {
                    case 8, 16, 32 -> v -> {
                        v.visitInsn(Opcodes.ICONST_M1);
                        v.visitInsn(Opcodes.IXOR);
                    };
                    case 64 -> v -> {
                        v.visitLdcInsn(-1L);
                        v.visitInsn(Opcodes.LXOR);
                    };
                    default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
                })),
//                DefineConstWithFallback.defineUnary("not", b -> b.equals(BigInteger.ZERO), boolType, doOperationThenConvert(v -> {
//                    Label pushTrue = new Label();
//                    Label end = new Label();
//                    if (bits == 64) {
//                        v.visitInsn(Opcodes.LCMP);
//                        v.visitInsn(Opcodes.ICONST_0);
//                    }
//                    v.visitJumpInsn(Opcodes.IFEQ, pushTrue);
//                    v.visitInsn(Opcodes.ICONST_0);
//                    v.visitJumpInsn(Opcodes.GOTO, end);
//                    v.visitLabel(pushTrue);
//                    v.visitInsn(Opcodes.ICONST_1);
//                    v.visitLabel(end);
//                })),

                //Comparisons
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("gt", (a, b) -> a.compareTo(b) > 0, type, boolType, intCompare(Opcodes.IF_ICMPGT)),
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("lt", (a, b) -> a.compareTo(b) < 0, type, boolType, intCompare(Opcodes.IF_ICMPLT)),
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("ge", (a, b) -> a.compareTo(b) >= 0, type, boolType, intCompare(Opcodes.IF_ICMPGE)),
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("le", (a, b) -> a.compareTo(b) <= 0, type, boolType, intCompare(Opcodes.IF_ICMPLE)),
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("eq", (a, b) -> a.compareTo(b) == 0, type, boolType, intCompare(Opcodes.IF_ICMPEQ))
        ));
    }

    private Consumer<MethodVisitor> doOperationThenConvert(Consumer<MethodVisitor> doOperation) {
        return v -> {
            doOperation.accept(v);
            switch (bits) {
                case 8 -> {
                    v.visitInsn(Opcodes.I2B);
                    if (!signed) {
                        v.visitLdcInsn(0xff);
                        v.visitInsn(Opcodes.IAND);
                    }
                }
                case 16 -> {
                    v.visitInsn(Opcodes.I2S);
                    if (!signed) {
                        v.visitLdcInsn(0xffff);
                        v.visitInsn(Opcodes.IAND);
                    }
                }
                case 32, 64 -> {}
            }
        };
    }

    private Consumer<MethodVisitor> intCompare(int intCompareOp) {
        return v -> {
            Label pushTrue = new Label();
            Label end = new Label();
            if (!signed) {
                //Add min_value to both args
                for (int i = 0; i < 2; i++) {
                    if (bits <= 32) {
                        switch (bits) {
                            case 8 -> v.visitIntInsn(Opcodes.BIPUSH, Byte.MIN_VALUE);
                            case 16 -> v.visitIntInsn(Opcodes.SIPUSH, Short.MIN_VALUE);
                            case 32 -> v.visitLdcInsn(Integer.MIN_VALUE);
                        }
                        v.visitInsn(Opcodes.IADD);
                        v.visitInsn(Opcodes.SWAP);
                    } else {
                        v.visitLdcInsn(Long.MIN_VALUE);
                        v.visitInsn(Opcodes.LADD);
                        BytecodeHelper.swapBigBig(v);
                    }
                }
            }
            if (bits == 64) {
                v.visitInsn(Opcodes.LCMP);
                v.visitInsn(Opcodes.ICONST_0);
            }
            v.visitJumpInsn(intCompareOp, pushTrue);
            v.visitInsn(Opcodes.ICONST_0);
            v.visitJumpInsn(Opcodes.GOTO, end);
            v.visitLabel(pushTrue);
            v.visitInsn(Opcodes.ICONST_1);
            v.visitLabel(end);
        };
    }

    @Override
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return descriptor;
    }

    @Override
    public String getRuntimeName(List<Type> generics, TypePool pool) {
        return null;
    }

    @Override
    public boolean extensible() {
        return false;
    }
}
