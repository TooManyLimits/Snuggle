package compile;

import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.UnitType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import exceptions.runtime.SnuggleException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import runtime.Unit;

/**
 * Class with several helpful methods for outputting bytecode
 */
public class BytecodeHelper {

    //u32 --> non-negative long with the same value
    public static void u32ToLong(MethodVisitor visitor) {
        visitor.visitInsn(Opcodes.I2L);
        visitor.visitLdcInsn(0xffffffffL);
        visitor.visitInsn(Opcodes.LAND);
    }

    public static void u16ToInt(MethodVisitor visitor) {
        visitor.visitLdcInsn(0xffff);
        visitor.visitInsn(Opcodes.IAND);
    }

    public static void u8ToInt(MethodVisitor visitor) {
        visitor.visitLdcInsn(0xff);
        visitor.visitInsn(Opcodes.IAND);
    }

    //Dup a value on the stack, and optionally send it down
    public static void dup(TypeDef typeDef, MethodVisitor visitor, int slotsToSendDown) {
        int dup2Op = switch (slotsToSendDown) {
            case 0 -> Opcodes.DUP2;
            case 1 -> Opcodes.DUP2_X1;
            case 2 -> Opcodes.DUP2_X2;
            default -> throw new IllegalStateException("SlotsToSendDown must be 0, 1, or 2");
        };
        int dupOp = switch (slotsToSendDown) {
            case 0 -> Opcodes.DUP;
            case 1 -> Opcodes.DUP_X1;
            case 2 -> Opcodes.DUP_X2;
            default -> throw new IllegalStateException("SlotsToSendDown must be 0, 1, or 2");
        };

        if (typeDef instanceof BuiltinTypeDef b) {
            if (b.builtin() instanceof IntegerType i && i.bits == 64)
                visitor.visitInsn(dup2Op);
            else if (b.builtin() instanceof FloatType f && f.bits == 64)
                visitor.visitInsn(dup2Op);
            else
                visitor.visitInsn(dupOp);
        } else {
            visitor.visitInsn(dupOp);
        }
    }

    //Pop an element from the stack
    public static void pop(TypeDef typeDef, MethodVisitor visitor) {
        //If output was a double or long, pop 2 slots
        if (typeDef instanceof BuiltinTypeDef b) {
            if (b.builtin() instanceof IntegerType i) {
                if (i.bits == 64) {
                    visitor.visitInsn(Opcodes.POP2);
                    return;
                }
            } else if (b.builtin() instanceof FloatType f) {
                if (f.bits == 64) {
                    visitor.visitInsn(Opcodes.POP2);
                    return;
                }
            }
        }
        //Otherwise, just pop 1
        visitor.visitInsn(Opcodes.POP);
    }

    public static void pushNone(TypeDef innerType, MethodVisitor visitor) {
        if (innerType.isReferenceType()) {
            visitor.visitInsn(Opcodes.ACONST_NULL);
        } else {
            visitor.visitInsn(Opcodes.ICONST_0); //TODO: Make this actually work
            //throw new IllegalStateException("Optional non-reference types not yet implemented");
        }
    }

    public static void createObject(MethodVisitor v, Class<?> classToCreate, Object... args) {
        String className = Type.getInternalName(classToCreate);
        v.visitTypeInsn(Opcodes.NEW, className);
        v.visitInsn(Opcodes.DUP);
        StringBuilder descriptor = new StringBuilder("(");
        for (var arg : args) {
            v.visitLdcInsn(arg);
            descriptor.append("L").append(Type.getInternalName(arg.getClass())).append(";");
        }
        descriptor.append(")V");
        v.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", descriptor.toString(), false);
    }


    //long, int --> int, long
    public static void swapBigSmall(MethodVisitor visitor) {
        visitor.visitInsn(Opcodes.DUP_X2);
        visitor.visitInsn(Opcodes.POP);
    }
    //long1, long2 --> long2, long1
    public static void swapBigBig(MethodVisitor visitor) {
        visitor.visitInsn(Opcodes.DUP2_X2);
        visitor.visitInsn(Opcodes.POP2);
    }
    //int, long --> long, int
    public static void swapSmallBig(MethodVisitor visitor) {
        visitor.visitInsn(Opcodes.DUP2_X1);
        visitor.visitInsn(Opcodes.POP2);
    }

    //Push unit on the stack
    private static final String unitName = Type.getInternalName(Unit.class);
    private static final String unitDescriptor = "L" + unitName + ";";
    public static void pushUnit(MethodVisitor visitor) {
        visitor.visitFieldInsn(Opcodes.GETSTATIC, unitName, "INSTANCE", unitDescriptor);
    }

}
