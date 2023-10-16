package builtin_types.reflect;

public class ReflectionUtils {
    
    public static String getDescriptor(Iterable<Class<?>> params, Class<?> returnType) {
        StringBuilder builder = new StringBuilder("(");
        for (Class<?> c : params)
            builder.append(getDescriptor(c));
        builder.append(")").append(getDescriptor(returnType));
        return builder.toString();
    }

    public static String getDescriptor(Class<?> c) {
        String className = c.getName().replace('.', '/');
        return switch (className) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            case "char" -> "C";
            case "void" -> "V";
            default -> "L" + className + ";";
        };
    }


}
