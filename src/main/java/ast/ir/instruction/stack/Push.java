package ast.ir.instruction.stack;

import ast.ir.instruction.Instruction;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.StringType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import ast.ir.helper.BytecodeHelper;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import runtime.Unit;
import util.Fraction;

import java.math.BigInteger;

//Push the given Object onto the stack
public record Push(Loc loc, Object obj, TypeDef type) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) throws CompilationException {
        Object obj = obj();
        TypeDef type = type().get();

        //Convert fractions ahead of time to either double or float, to save code
        if (obj instanceof Fraction f) {
            if (type.builtin() instanceof FloatType t)
                obj = t.bits == 32 ? f.floatValue() : f.doubleValue();
            else
                throw new IllegalStateException("Literal obj is fraction, but type is not float type? Bug in compiler, please report!");
        }

        //Push the value on the stack!
        if (obj instanceof BigInteger v) {
            if (type.builtin() instanceof IntegerType i) {
                if (!i.fits(v))
                    throw new TypeCheckingException("Expected " + i.name() + ", but integer literal " + v + " is out of range. (" + i.min + " to " + i.max + ")", loc);
                if (i.bits <= 32) {
                    if (!IntegerType.I16.fits(v))
                        jvm.visitLdcInsn(v.intValue());
                    else if (!IntegerType.I8.fits(v))
                        jvm.visitIntInsn(Opcodes.SIPUSH, v.shortValue());
                    else if (v.equals(BigInteger.ZERO))
                        jvm.visitInsn(Opcodes.ICONST_0);
                    else if (v.equals(BigInteger.ONE))
                        jvm.visitInsn(Opcodes.ICONST_1);
                    else if (v.equals(BigInteger.TWO))
                        jvm.visitInsn(Opcodes.ICONST_2);
                    else if (v.equals(BigInteger.valueOf(3)))
                        jvm.visitInsn(Opcodes.ICONST_3);
                    else if (v.equals(BigInteger.valueOf(4)))
                        jvm.visitInsn(Opcodes.ICONST_4);
                    else if (v.equals(BigInteger.valueOf(5)))
                        jvm.visitInsn(Opcodes.ICONST_5);
                    else if (v.equals(BigInteger.valueOf(-1)))
                        jvm.visitInsn(Opcodes.ICONST_M1);
                    else
                        jvm.visitIntInsn(Opcodes.BIPUSH, v.byteValue());
                } else {
                    if (v.equals(BigInteger.ZERO))
                        jvm.visitInsn(Opcodes.LCONST_0);
                    else if (v.equals(BigInteger.ONE))
                        jvm.visitInsn(Opcodes.LCONST_1);
                    else
                        jvm.visitLdcInsn(v.longValue());
                }
            } else
                throw new IllegalStateException("Literal obj is big int, but type is not integer type? Bug in compiler, please report!");
        } else if (obj instanceof String s) {
            if (type.builtin() == StringType.INSTANCE)
                jvm.visitLdcInsn(s);
            else
                throw new IllegalStateException("Literal obj is string, but type is not StringType? Bug in compiler, please report!");
        } else if (obj instanceof Boolean b) {
            jvm.visitInsn(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        } else if (obj == Unit.INSTANCE) {
            BytecodeHelper.pushUnit(jvm);
        } else if (obj instanceof Float f) {
            if (f == 0)
                jvm.visitInsn(Opcodes.FCONST_0);
            else if (f == 1)
                jvm.visitInsn(Opcodes.FCONST_1);
            else if (f == 2)
                jvm.visitInsn(Opcodes.FCONST_2);
            else
                jvm.visitLdcInsn(f);
        } else if (obj instanceof Double d) {
            if (d == 0)
                jvm.visitInsn(Opcodes.DCONST_0);
            else if (d == 1)
                jvm.visitInsn(Opcodes.DCONST_1);
            else
                jvm.visitLdcInsn(d);
        } else {
            throw new IllegalStateException("Unrecognized literal obj type: " + obj.getClass().getName() + ". Bug in compiler, please report!");
        }
    }

    @Override
    public int cost() {
        return 1; //Cost is always considered 1
    }
}
