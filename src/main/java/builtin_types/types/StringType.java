package builtin_types.types;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class StringType implements BuiltinType {

    public static final StringType INSTANCE = new StringType();
    private StringType() {}

    @Override
    public String name() {
        return "String";
    }

    @Override
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return "Ljava/lang/String;";
    }

    @Override
    public String getRuntimeName(List<Type> generics, TypePool pool) {
        return "java/lang/String";
    }


    @Override
    public List<? extends MethodDef> getMethods(List<Type> generics, TypePool pool) throws CompilationException {
        Type stringType = pool.getBasicBuiltin(INSTANCE);
        return List.of(
                new BytecodeMethodDef(false, "add", List.of(stringType), stringType, v -> {
                    v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
                })
        );
    }

    @Override
    public boolean extensible() {
        return false;
    }
}
