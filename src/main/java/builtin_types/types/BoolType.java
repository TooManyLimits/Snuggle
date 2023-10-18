package builtin_types.types;

import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.MethodDef;
import builtin_types.BuiltinType;
import builtin_types.helpers.DefineConstWithFallback;
import exceptions.CompilationException;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

import java.util.List;

public class BoolType implements BuiltinType {

    public static final BoolType INSTANCE = new BoolType();
    private BoolType() {}

    @Override
    public String name() {
        return "bool";
    }

    @Override
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return "Z";
    }

    @Override
    public List<? extends MethodDef> getMethods(List<Type> generics, TypePool pool) throws CompilationException {
        Type boolType = pool.getBasicBuiltin(INSTANCE);

        return ListUtils.join(List.of(
                //eq
                DefineConstWithFallback.<Boolean, Boolean, Boolean>defineBinary("eq", Boolean::equals, boolType, boolType, v -> {
                    v.visitInsn(Opcodes.IXOR);
                    v.visitInsn(Opcodes.ICONST_1);
                    v.visitInsn(Opcodes.IXOR);
                }),
                //not
                DefineConstWithFallback.<Boolean, Boolean>defineUnary("not", a -> !a, boolType, v -> {
                    v.visitInsn(Opcodes.ICONST_1);
                    v.visitInsn(Opcodes.IXOR);
                })
        ));
    }

    @Override
    public String getRuntimeName(List<Type> generics, TypePool pool) {
        return null;
    }

    @Override
    public boolean extensible() {
        return false;
    }
}
