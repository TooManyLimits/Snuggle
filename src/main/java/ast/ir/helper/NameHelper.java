package ast.ir.helper;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import util.ListUtils;

/**
 * Manages the various topLevelTypes during the compiler stage;
 * Particularly, keeps track of various generated generatedClasses and their compiled java-side names.
 */
public class NameHelper {

    //Helpers to generate certain strings given from a number.
    public static String getRuntimeClassName() { return "snuggle/Runtime"; }
    public static String getFilesClassName() { return "snuggle/Files"; }
    public static String getImportMethodName(String fileName) { return "importFile_" + fileName; }
    public static String getImportFieldName(String fileName) { return "hasImported_" + fileName; }

    //If ASM throws a cryptic error, enable this to figure out what's wrong
    public static final boolean DEBUG_BYTECODE_GENERATION = true;

    //Helper to generate and set up a class writer with the given parameters
    //Adds a default constructor, assuming the supertype also has a default constructor
    public static ClassVisitor generateWriter(String name, String supertypeName, boolean defaultConstructor, boolean isInterface, String... interfaces) {
        ClassVisitor writer = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object"; //Cursed
            }
        };
        if (DEBUG_BYTECODE_GENERATION)
            writer = new CheckClassAdapter(writer);
        int version = Opcodes.V17; //version 61.0
        int access = Opcodes.ACC_PUBLIC + (isInterface ? Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT : 0);
        writer.visit(version, access, name, null, supertypeName, interfaces);

        //Add a default constructor if asked
        if (defaultConstructor) {
            MethodVisitor constructor = writer.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    "<init>",
                    "()V",
                    null,
                    null
            );
            constructor.visitCode();
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, supertypeName, "<init>", "()V", false);
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(0, 0); //Auto compute
            constructor.visitEnd();
        }

        return writer;
    }

    public static ClassVisitor generateClassWriter(String name, String supertypeName, boolean defaultConstructor, Class<?>... interfaces) {
        String[] interfacesMapped = ListUtils.mapArray(interfaces, String.class, org.objectweb.asm.Type::getInternalName);
        return generateWriter(name, supertypeName, defaultConstructor, false, interfacesMapped);
    }

    public static ClassVisitor generateClassWriter(String name, boolean defaultConstructor, Class<?>... interfaces) {
        String[] interfacesMapped = ListUtils.mapArray(interfaces, String.class, org.objectweb.asm.Type::getInternalName);
        return generateWriter(name, "java/lang/Object", defaultConstructor, false, interfacesMapped);
    }

    public static ClassVisitor generateInterfaceWriter(String name, Class<?>... interfaces) {
        String[] interfacesMapped = ListUtils.mapArray(interfaces, String.class, org.objectweb.asm.Type::getInternalName);
        return generateWriter(name, "java/lang/Object", false, true, interfacesMapped);
    }

}
