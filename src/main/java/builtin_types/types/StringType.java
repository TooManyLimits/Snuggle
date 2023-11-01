package builtin_types.types;

import ast.passes.TypeChecker;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

public class StringType implements BuiltinType {

    public static final StringType INSTANCE = new StringType();
    private StringType() {}

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics) {
        TypeDef stringType = checker.getBasicBuiltin(INSTANCE);
        return List.of(
                new BytecodeMethodDef("add", false, stringType, List.of(stringType), stringType, false, v -> {
                    v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
                })
        );
    }

    @Override
    public String name() {
        return "String";
    }

    @Override
    public String runtimeName(TypeChecker checker, List<TypeDef> generics) {
        return Type.getInternalName(String.class);
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return List.of(Type.getDescriptor(String.class));
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return Type.getDescriptor(String.class);
    }

    @Override
    public boolean isReferenceType(TypeChecker checker, List<TypeDef> generics) {
        return true;
    }

    @Override
    public boolean isPlural(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public boolean extensible(TypeChecker checker, List<TypeDef> generics) {
        return false;
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        return 1;
    }

    @Override
    public TypeDef getInheritanceSupertype(TypeChecker checker, List<TypeDef> generics) {
        return checker.getBasicBuiltin(ObjType.INSTANCE);
    }
}
