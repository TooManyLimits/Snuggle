package ast.ir.helper;

import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.ArrayType;
import builtin_types.types.BoolType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import runtime.Unit;
import util.ListUtils;

import java.util.List;
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
                AtomicInteger mutableIndex = new AtomicInteger(index + def.stackSlots()); //cursed
                ListUtils.iterBackwards(def.fields(), field -> {
                    if (field.isStatic()) return;
                    mutableIndex.addAndGet(-field.type().stackSlots());
                    visitVariable(mutableIndex.get(), field.type(), store, visitor);
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

    public static void pushDefaultValue(MethodVisitor jvm, TypeDef def) {
        if (def.isPlural()) {
            for (FieldDef field : def.fields())
                pushDefaultValue(jvm, field.type());
        } else if (def.isReferenceType()) {
            jvm.visitInsn(Opcodes.ACONST_NULL);
        } else if (def.builtin() instanceof IntegerType i) {
            switch (i.bits) {
                case 8, 16, 32 -> jvm.visitInsn(Opcodes.ICONST_0);
                case 64 -> jvm.visitInsn(Opcodes.LCONST_0);
                default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
            }
        } else if (def.builtin() instanceof BoolType) {
            jvm.visitInsn(Opcodes.ICONST_0);
        } else if (def.builtin() instanceof FloatType f) {
            switch (f.bits) {
                case 32 -> jvm.visitInsn(Opcodes.FCONST_0);
                case 64 -> jvm.visitInsn(Opcodes.DCONST_0);
                default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
            }
        } else {
            throw new IllegalStateException("Unrecognized type \"" + def.name() + "\" - didn't meet any condition? Bug in compiler, please report!");
        }
    }

    //Size starts on the stack. At the end, the array(s) then the size are on the stack.
    public static void newArray(MethodVisitor jvm, TypeDef elemType) {
        if (elemType.isPlural()) {
            for (FieldDef field : elemType.fields()) {
                newArray(jvm, field.type()); //[some arrays, size]
            }
        } else if (elemType.isReferenceType() || elemType.isOptionalReferenceType()) {
            jvm.visitInsn(Opcodes.DUP);
            jvm.visitTypeInsn(Opcodes.ANEWARRAY, elemType.runtimeName());
            jvm.visitInsn(Opcodes.SWAP);
        } else if (elemType.builtin() instanceof IntegerType i) {
            jvm.visitInsn(Opcodes.DUP);
            switch (i.bits) {
                case 8 -> jvm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
                case 16 -> jvm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT);
                case 32 -> jvm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
                case 64 -> jvm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
                default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
            }
            jvm.visitInsn(Opcodes.SWAP);
        } else if (elemType.builtin() instanceof BoolType) {
            jvm.visitInsn(Opcodes.DUP);
            jvm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
            jvm.visitInsn(Opcodes.SWAP);
        } else if (elemType.builtin() instanceof FloatType f) {
            jvm.visitInsn(Opcodes.DUP);
            switch (f.bits) {
                case 32 -> jvm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
                case 64 -> jvm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
                default -> throw new IllegalStateException("Illegal bit count, bug in compiler, please report!");
            }
            jvm.visitInsn(Opcodes.SWAP);
        } else {
            throw new IllegalStateException("Unrecognized type \"" + elemType.name() + "\" - didn't meet any condition? Bug in compiler, please report!");
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

    //[first, second] -> [second, first]
    public static void swap(MethodVisitor visitor, TypeDef first, TypeDef second) {
        int firstSlots = first.stackSlots();
        int secondSlots = second.stackSlots();
        if (firstSlots == 0 || secondSlots == 0) return;
        if (firstSlots > 2 || secondSlots > 2) throw new IllegalArgumentException("BytecodeHelper.swap() only expects elements with 1 or 2 slots, got " + Math.max(firstSlots, secondSlots));
        if (firstSlots == 1)
            if (secondSlots == 1)
                visitor.visitInsn(Opcodes.SWAP);
            else
                swapSmallBig(visitor);
        else
            if (secondSlots == 1)
                swapBigSmall(visitor);
            else
                swapBigBig(visitor);
    }


    //Push unit on the stack
    private static final String unitName = Type.getInternalName(Unit.class);
    private static final String unitDescriptor = "L" + unitName + ";";
    public static void pushUnit(MethodVisitor visitor) {
        visitor.visitFieldInsn(Opcodes.GETSTATIC, unitName, "INSTANCE", unitDescriptor);
    }

}
