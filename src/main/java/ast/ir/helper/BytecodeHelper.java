package ast.ir.helper;

import ast.typed.def.field.FieldDef;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.BoolType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import runtime.Unit;
import util.ListUtils;

import java.util.concurrent.atomic.AtomicInteger;

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

    public static void visitVariable(int index, TypeDef def, boolean store, MethodVisitor visitor) {
        //Handle plural types first
        if (def.isPlural()) {
            if (store) {
                AtomicInteger mutableIndex = new AtomicInteger(index); //cursed
                ListUtils.iterBackwards(def.fields(), field -> {
                    if (field.isStatic()) return;
                    visitVariable(mutableIndex.get(), field.type(), store, visitor);
                    mutableIndex.addAndGet(field.type().stackSlots());
                });
            } else {
                for (FieldDef field : def.fields()) {
                    if (field.isStatic()) continue;
                    visitVariable(index, field.type(), store, visitor);
                    index += field.type().stackSlots();
                }
            }
        }
        //Now other types
        else if (def.builtin() instanceof IntegerType i) {
            switch (i.bits) {
                case 8, 16, 32 -> visitor.visitVarInsn(store ? Opcodes.ISTORE : Opcodes.ILOAD, index);
                case 64 -> visitor.visitVarInsn(store ? Opcodes.LSTORE : Opcodes.LLOAD, index);
                default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
            }
        } else if (def.builtin() instanceof FloatType f) {
            switch (f.bits) {
                case 32 -> visitor.visitVarInsn(store ? Opcodes.FSTORE : Opcodes.FLOAD, index);
                case 64 -> visitor.visitVarInsn(store ? Opcodes.DSTORE : Opcodes.DLOAD, index);
                default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
            }
        } else if (def.builtin() == BoolType.INSTANCE) {
            visitor.visitVarInsn(store ? Opcodes.ISTORE : Opcodes.ILOAD, index);
        } else {
            //Assumed all others are reference types
            visitor.visitVarInsn(store ? Opcodes.ASTORE :Opcodes.ALOAD, index);
        }
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
