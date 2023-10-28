package builtin_types.reflect;


import builtin_types.reflect.annotations.Unsigned;
import ast.ir.helper.NameHelper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

//Note: This class DOESN'T WORK.
//It was made for testing to see if it worked, and turns out it didn't.
//Still keeping the code here for reference in case we want it in the future.

//Java cannot normally create unsigned byte/short, so this class
//is for if you want to return them in a reflected method.
public abstract class Convert {

//    private static final Convert INSTANCE;
//    private static final CustomLoader customLoader; //custom classloader needed to hold ASM generated class
//
//    //These @Unsigned annotations don't do anything, they're
//    //just for clarity's sake
//    protected abstract @Unsigned short convertToU16(int value);
//    protected abstract @Unsigned byte convertToU8(int value);
//
//    //Create an unsigned byte of the given value
//    public static @Unsigned byte u8(int value) {
//        return INSTANCE.convertToU8(value);
//    }
//
//    //Create an unsigned short of the given value
//    public static @Unsigned short u16(int value) {
//        return INSTANCE.convertToU16(value);
//    }
//
//    //Creating the instance...
//    static {
//        //Get the class writer
//        ClassWriter writer = NameHelper.generateClassWriter("snuggle/builtin_types/reflect/Convert$$INSTANCE$$", Type.getInternalName(Convert.class));
//        //Constructor
//        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
//        constructor.visitCode();
//        constructor.visitVarInsn(Opcodes.ALOAD, 0);
//        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Convert.class), "<init>", "()V", false);
//        constructor.visitInsn(Opcodes.RETURN);
//        constructor.visitMaxs(0, 0);
//        constructor.visitEnd();
//        //u8 method
//        MethodVisitor u8 = writer.visitMethod(Opcodes.ACC_PROTECTED, "convertToU8", "(I)B", null, null);
//        u8.visitCode();
//        u8.visitVarInsn(Opcodes.ILOAD, 1);
//        u8.visitIntInsn(Opcodes.SIPUSH, 0xFF);
//        u8.visitInsn(Opcodes.IAND);
//        u8.visitInsn(Opcodes.IRETURN);
//        u8.visitMaxs(0, 0);
//        u8.visitEnd();
//        //u16 method
//        MethodVisitor u16 = writer.visitMethod(Opcodes.ACC_PROTECTED, "convertToU16", "(I)S", null, null);
//        u16.visitCode();
//        u16.visitVarInsn(Opcodes.ILOAD, 1);
//        u16.visitLdcInsn(0xFFFF);
//        u16.visitInsn(Opcodes.IAND);
//        u16.visitInsn(Opcodes.IRETURN);
//        u16.visitMaxs(0, 0);
//        u16.visitEnd();
//
//        //End writer, define the class, and instantiate
//        writer.visitEnd();
//        customLoader = new CustomLoader();
//        Convert instance;
//        try {
//            instance = (Convert) customLoader.define(writer.toByteArray()).getConstructor().newInstance();
//        } catch (Throwable e) {
//            throw new IllegalStateException("Failed to instantiate unsigned converter instance", e);
//        }
//        INSTANCE = instance;
//    }
//
//    private static class CustomLoader extends ClassLoader {
//        public Class<?> define(byte[] bytes) {
//            return defineClass(null, bytes, 0, bytes.length);
//        }
//    }


}
