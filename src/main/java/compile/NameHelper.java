package compile;

import ast.typed.Type;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

import java.util.List;

/**
 * Manages the various types during the compiler stage;
 * Particularly, keeps track of various generated classes and their compiled java-side names.
 */
public class NameHelper {

    //Helpers to generate certain strings given from a number.
    public static String getRuntimeClassName(int instanceId) { return "snuggle/SnuggleGeneratedClass_" + instanceId + "_Runtime"; }
    public static String getFilesClassName(int instanceId) { return "snuggle/SnuggleGeneratedClass_" + instanceId + "_Files"; }

    public static String getSnuggleClassName(int index) { return "snuggle/SnuggleGeneratedClass_" + index; }

    public static String getImportMethodName(int fileId, String fileName) { return "snuggleGeneratedImportMethod_" + fileId + "_name_" + fileName; }
    public static String getImportFieldName(int fileId) { return "snuggleGeneratedImportField_" + fileId; }

    public static String getMethodName(String name, List<Type> paramTypes, Type returnType) {
        StringBuilder s = new StringBuilder("snuggle_generated_method_");
        for (Type t : paramTypes) {
            if (t instanceof Type.Basic b)
                s.append(b.index()).append("_");
            else
                throw new IllegalStateException("Should not be trying to get generated methodName of generic method? Bug in compiler, please report!");
        }

        if (returnType instanceof Type.Basic b)
            return s.append("to_").append(b.index()).append("_name_").append(name).toString();

        throw new IllegalStateException("Should not be trying to get generated methodName of generic method? Bug in compiler, please report!");
    }

    public static String getFieldName(String name, Type type) {
        StringBuilder s = new StringBuilder("snuggle_generated_field_");
        if (type instanceof Type.Basic b)
            return s.append(b.index()).append("_name_").append(name).toString();
        throw new IllegalStateException("Should not be trying to get generated fieldName of generic field? Bug in compiler, please report!");
    }

    //Helper to generate and set up a class writer with the given parameters
    public static ClassWriter generateClassWriter(String name, String supertypeName, Class<?>... interfaces) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        int version = Opcodes.V17; //version 61.0
        int access = Opcodes.ACC_PUBLIC;
        String[] interfacesMapped = ListUtils.mapArray(interfaces, String.class, org.objectweb.asm.Type::getInternalName);
        writer.visit(version, access, name, null, supertypeName, interfacesMapped);

        return writer;
    }

    public static ClassWriter generateClassWriter(String name, Class<?>... interfaces) {
        return generateClassWriter(name, "java/lang/Object", interfaces);
    }

}
