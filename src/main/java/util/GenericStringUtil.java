package util;

import ast.typed.def.type.TypeDef;

import java.util.List;

public class GenericStringUtil {

    public static String instantiateName(String name, List<TypeDef> generics, String leftBracket, String rightBracket) {
        StringBuilder newName = new StringBuilder(name);
        if (generics.size() > 0) {
            newName.append(leftBracket);
            for (TypeDef t : generics) {
                newName.append(t.name());
                newName.append(", ");
            }
            newName.delete(newName.length() - 2, newName.length());
            newName.append(rightBracket);
        }
        return newName.toString();
    }

    public static String instantiateName(String name, List<TypeDef> generics) {
        return instantiateName(name, generics, "(", ")");
    }

    //Cursed. Horrifying if you will
    //Use this whenever we send something into an actual
    //JVM-adjacent method, like something from ASM. We don't
    //need to mangle until then.

    //ClassVisitor.visitField/visitMethod()
    //MethodVisitor.visitFieldInsn/visitMethodInsn()
    public static String mangleSlashes(String name) {
        return name.replace('/', '\\');
    }

}
