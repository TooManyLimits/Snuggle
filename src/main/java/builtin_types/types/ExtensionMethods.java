package builtin_types.types;

import ast.passes.TypeChecker;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;

import java.util.List;

/**
 * Just exists as a place to throw all the various extension methods that exist.
 */
public class ExtensionMethods implements BuiltinType {

    public static final ExtensionMethods INSTANCE = new ExtensionMethods();
    private ExtensionMethods() {}

    @Override
    public String name() {
        return "ExtensionMethods";
    }

    @Override
    public boolean nameable() {
        return false;
    }

    @Override
    public String runtimeName(TypeChecker checker, List<TypeDef> generics) {
        return "snuggle/ExtensionMethods";
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return null;
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return null;
    }

    @Override
    public boolean shouldGenerateStructClassAtRuntime(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public boolean isReferenceType(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public boolean isPlural(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public boolean extensible(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        return 0;
    }
}
