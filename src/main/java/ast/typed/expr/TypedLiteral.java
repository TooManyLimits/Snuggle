package ast.typed.expr;

import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.types.StringType;
import builtin_types.types.numbers.IntegerType;
import compile.Compiler;
import compile.ScopeHelper;
import exceptions.CompilationException;
import exceptions.TypeCheckingException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import runtime.Unit;

import java.math.BigDecimal;
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
                    visitor.visitLdcInsn(v.longValue());
                }
            } else
                throw new IllegalStateException("Literal obj is big int, but type is not integer type? Bug in compiler, please report!");
        } else if (obj instanceof String s) {
            if (compiler.getTypeDef(type) instanceof BuiltinTypeDef b && b.builtin() == StringType.INSTANCE)
                visitor.visitLdcInsn(s);
            else
                throw new IllegalStateException("Literal obj is string, but type is not StringType? Bug in compiler, please report!");
        } else if (obj == Unit.INSTANCE) {
            String unit = org.objectweb.asm.Type.getInternalName(Unit.class);
            visitor.visitFieldInsn(Opcodes.GETSTATIC, unit, "INSTANCE", "L" + unit + ";");
        } else if (obj instanceof Boolean b) {
            visitor.visitInsn(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        } else {
            throw new IllegalStateException("Unrecognized literal obj type: " + obj.getClass().getName() + ". Bug in compiler, please report!");
        }
    }

}
