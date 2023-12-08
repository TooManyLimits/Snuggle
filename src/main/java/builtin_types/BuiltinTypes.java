package builtin_types;

import builtin_types.reflect.Reflector;
import builtin_types.types.*;
import builtin_types.types.primitive.*;
import builtin_types.types.reflected.SystemType;
import util.IOUtil;

import java.nio.file.Path;
import java.util.*;

/**
 * Contains a set of builtin topLevelTypes which will be accessible from
 * a program. Pass an instance of this, with the appropriate topLevelTypes,
 * to the TypeResolver stage of compilation.
 */
public class BuiltinTypes {

    private final Set<BuiltinType> registeredTypes = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<String, String> registeredSnuggleFiles = new HashMap<>();
    private final List<String> autoImports = new ArrayList<>();

    private BuiltinTypes() {}

    public BuiltinTypes addType(BuiltinType builtinType) {
        registeredTypes.add(builtinType);
        return this;
    }

    public BuiltinTypes reflectType(Class<?> clazz) {
        return addType(Reflector.reflect(clazz));
    }

    public BuiltinTypes addFile(String fileName, String snuggleSourceCode) {
        registeredSnuggleFiles.put(fileName, snuggleSourceCode);
        return this;
    }

    public BuiltinTypes removeType(BuiltinType builtinType) {
        registeredTypes.remove(builtinType);
        return this;
    }

    public BuiltinTypes removeReflectedType(Class<?> clazz) {
        registeredTypes.remove(Reflector.reflect(clazz));
        return this;
    }

    public BuiltinTypes removeFile(String fileName) {
        registeredSnuggleFiles.remove(fileName);
        return this;
    }

    public BuiltinTypes autoImport(String fileName) {
        autoImports.add(fileName);
        return this;
    }

    public BuiltinTypes removeAutoImport(String fileName) {
        autoImports.remove(fileName);
        return this;
    }

    //Constructors and helpers

    //Add the standard types to a new instance and return it.
    public static BuiltinTypes standard() {
        return new BuiltinTypes()
                //object and string
                .addType(ObjType.INSTANCE)
                .addType(StringType.INSTANCE)//.autoImportResource("std/StringExtensions")
                //Primitives
                .addType(BoolType.INSTANCE)
                .addType(CharType.INSTANCE)
                .addType(IntLiteralType.INSTANCE)
                .addType(IntegerType.I8)
                .addType(IntegerType.U8)
                .addType(IntegerType.I16)
                .addType(IntegerType.U16)
                .addType(IntegerType.I32)
                .addType(IntegerType.U32)
                .addType(IntegerType.I64)
                .addType(IntegerType.U64)
                .addType(FloatLiteralType.INSTANCE)
                .addType(FloatType.F32)
                .addType(FloatType.F64)
                //Options and arrays
                .addType(OptionType.INSTANCE)
                .addType(ArrayType.INSTANCE)
                //Extension methods holder
                .addType(ExtensionMethods.INSTANCE)

                //Additional types
                .addType(MaybeUninit.INSTANCE)
                .reflectType(SystemType.class)

                //Standard lib
                .addStandardLibrary()
        ;
    }

    private BuiltinTypes addStandardLibrary() {
        try {
            Path p = Path.of(Thread.currentThread().getContextClassLoader().getResource("std").toURI());
            IOUtil.applyRecursive(p, path -> {
                String name = "std/" + p.relativize(path).toString().replace('\\', '/');
                if (!name.endsWith(".snuggle"))
                    throw new IllegalStateException("Files in standard library should end with .snuggle, but got \"" + name + "\"");
                String withoutExtension = name.substring(0, name.length() - ".snuggle".length());
                addFile(withoutExtension, IOUtil.getResource(name));
                //Auto-import extensions
                if (name.startsWith("std/extensions/"))
                    autoImport(withoutExtension);
            });
            return this;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to import standard library for snuggle", e);
        }
    }

    public Collection<BuiltinType> getBuiltins() {
        return registeredTypes;
    }

    public Map<String, String> getSnuggleFiles() {
        return registeredSnuggleFiles;
    }

    public List<String> getAutoImports() {
        return autoImports;
    }

}
