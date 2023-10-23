package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import builtin_types.types.StringType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import compile.BytecodeHelper;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import runtime.Unit;
import util.Fraction;

import java.math.BigInteger;

public record TypedLiteral(Loc loc, Object obj, Type type) implements TypedExpr {

    /**
     * Used in the circumstance of type() being a non-storable type (like a literal),
     * when a concrete type is expected. Literals cannot exist at runtime, so they need to be
     * "pulled up" into their supertype in order for them to be compiled. For example:
     * - IntLiteral -> i32
     * - FloatLiteral -> f64
     * - StringLiteral -> String
     * Assume that this.type() is a subtype of expected; checks have already been made.
     */
    public TypedLiteral pullTypeUpwards(Type expected) {
        return new TypedLiteral(loc, obj, expected);
    }

    @Override
    public void compile(Compiler compiler, ScopeHelper env, MethodVisitor visitor) throws CompilationException {
        Object obj = obj();

        //Convert fractions ahead of time to either double or float, to save code
        if (obj instanceof Fraction f) {
            if (compiler.getTypeDef(type) instanceof BuiltinTypeDef b && b.builtin() instanceof FloatType t)
                obj = t.bits == 32 ? f.floatValue() : f.doubleValue();
            else
                throw new IllegalStateException("Literal obj is fraction, but type is not float type? Bug in compiler, please report!");
        }

        //Push the value on the stack!

        if (obj instanceof BigInteger v) {
            if (compiler.getTypeDef(type) instanceof BuiltinTypeDef b && b.builtin() instanceof IntegerType i) {
                if (!i.fits(v))
                    throw new TypeCheckingException("Expected " + i.name() + ", but integer literal " + v + " is out of range. (" + i.min + " to " + i.max + ")", loc);
                if (i.bits <= 32) {
                    if (!IntegerType.I16.fits(v))
                        visitor.visitLdcInsn(v.intValue());
                    else if (!IntegerType.I8.fits(v))
                        visitor.visitIntInsn(Opcodes.SIPUSH, v.shortValue());
                    else if (v.equals(BigInteger.ZERO))
                        visitor.visitInsn(Opcodes.ICONST_0);
                    else if (v.equals(BigInteger.ONE))
                        visitor.visitInsn(Opcodes.ICONST_1);
                    else if (v.equals(BigInteger.TWO))
                        visitor.visitInsn(Opcodes.ICONST_2);
                    else if (v.equals(BigInteger.valueOf(3)))
                        visitor.visitInsn(Opcodes.ICONST_3);
                    else if (v.equals(BigInteger.valueOf(4)))
                        visitor.visitInsn(Opcodes.ICONST_4);
                    else if (v.equals(BigInteger.valueOf(5)))
                        visitor.visitInsn(Opcodes.ICONST_5);
                    else if (v.equals(BigInteger.valueOf(-1)))
                        visitor.visitInsn(Opcodes.ICONST_M1);
                    else
                        visitor.visitIntInsn(Opcodes.BIPUSH, v.byteValue());
                } else {
                    if (v.equals(BigInteger.ZERO))
                        visitor.visitInsn(Opcodes.LCONST_0);
                    else if (v.equals(BigInteger.ONE))
                        visitor.visitInsn(Opcodes.LCONST_1);
                    else
                        visitor.visitLdcInsn(v.longValue());
                }
            } else
                throw new IllegalStateException("Literal obj is big int, but type is not integer type? Bug in compiler, please report!");
        } else if (obj instanceof String s) {
            if (compiler.getTypeDef(type) instanceof BuiltinTypeDef b && b.builtin() == StringType.INSTANCE)
                visitor.visitLdcInsn(s);
            else
                throw new IllegalStateException("Literal obj is string, but type is not StringType? Bug in compiler, please report!");
        } else if (obj instanceof Boolean b) {
            visitor.visitInsn(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        } else if (obj == Unit.INSTANCE) {
            BytecodeHelper.pushUnit(visitor);
        } else if (obj instanceof Float f) {
            if (f == 0)
                visitor.visitInsn(Opcodes.FCONST_0);
            else if (f == 1)
                visitor.visitInsn(Opcodes.FCONST_1);
            else if (f == 2)
                visitor.visitInsn(Opcodes.FCONST_2);
            else
                visitor.visitLdcInsn(f);
        } else if (obj instanceof Double d) {
            if (d == 0)
                visitor.visitInsn(Opcodes.DCONST_0);
            else if (d == 1)
                visitor.visitInsn(Opcodes.DCONST_1);
            else
                visitor.visitLdcInsn(d);
        } else {
            throw new IllegalStateException("Unrecognized literal obj type: " + obj.getClass().getName() + ". Bug in compiler, please report!");
        }
    }

}
