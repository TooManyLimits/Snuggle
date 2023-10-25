package runtime;

import compile.Compiler;
import exceptions.runtime.SnuggleException;
import util.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that helps deal with SnuggleException
 * error reporting.
 * Each SnuggleInstance has one.
 */
public class ErrorHelper {

    //Maps mangled class names (SnuggleGeneratedClass_x) to their original names prefixed with the file name.
    private final Map<String, String> classUnmangleMap = new HashMap<>();
    private final ClassLoader loader;

    public ErrorHelper(Compiler.CompileResult result, ClassLoader loader) {
        //Store fields
        this.loader = loader;
        //Store un-mangled class names
        classUnmangleMap.put(result.runtime().mangledName(), result.runtime().name());
        result.otherClasses().forEach(c -> classUnmangleMap.put(c.mangledName(), c.name()));
        //Also add the builtin unmangle map
        classUnmangleMap.putAll(result.builtinMangleMap());
    }

    /**
     * Translate the given exception into a more readable form.
     */
    public SnuggleException translate(ClassCastException e) {
        //ClassCastException message is of the form: "class <class1> cannot be cast to class <class2> ..."
        //We want to isolate those class messages.
        String[] splitMessage = e.getMessage().split(" ");
        String class1 = unmangleClass(splitMessage[1]);
        String class2 = unmangleClass(splitMessage[7]);
        //Create a new exception:
        SnuggleException result = new SnuggleException("Could not cast to " + class2 + " because the type was " + class1);
        //Translate its stack trace
        result.setStackTrace(translate(e.getStackTrace()));
        return result;
    }

    public SnuggleException translate(StackOverflowError e) {
        SnuggleException result = new SnuggleException("Stack overflow!");
        result.setStackTrace(translate(e.getStackTrace()));
        return result;
    }

    public SnuggleException translate(SnuggleException e) {
        e.setStackTrace(translate(e.getStackTrace()));
        return e;
    }


    private String unmangleClass(String mangled) {
        mangled = mangled.replace('.', '/');
        String unmangled = classUnmangleMap.get(mangled);
        if (unmangled == null)
            throw new IllegalStateException("Class with mangled name \"" + mangled + "\" does not know its unmangled form? Bug in compiler, please report!");
        int lastSlashIndex = unmangled.lastIndexOf('/');
        if (lastSlashIndex == -1)
            return unmangled;
        return unmangled.substring(0, lastSlashIndex) + "." + unmangled.substring(lastSlashIndex + 1);
    }

    //Methods are suffixed with "name_[the method name]", so just grab that ending.
    private String unmangleMethodName(String mangled) {
        int index = mangled.indexOf("name_") + "name_".length();
        if (index == 4) {
            return mangled;
        }
        return mangled.substring(index);
    }


    public StackTraceElement[] translate(StackTraceElement[] stackTrace) {
        List<StackTraceElement> result = new ArrayList<>();
        for (StackTraceElement elem : stackTrace) {
            //If name doesn't match, end right here
            if (!elem.getClassLoaderName().equals(loader.getName()))
                break;
            //Otherwise, add this elem to the list
            String unmangledClassName = unmangleClass(elem.getClassName());
            int lastSlashIndex = unmangledClassName.lastIndexOf('/');
            String fileName = lastSlashIndex != -1 ? unmangledClassName.substring(0, lastSlashIndex) : unmangledClassName;
            result.add(new StackTraceElement(
                    unmangledClassName,
                    unmangleMethodName(elem.getMethodName()),
                    fileName.equals("Files") ? "line" : fileName,
                    elem.getLineNumber()
            ));
        }
        result.remove(result.size() - 1); //Remove last, as its info is not important

        return result.toArray(new StackTraceElement[0]);
    }




}
