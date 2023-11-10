package builtin_types.types;

import ast.passes.TypeChecker;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.helpers.DefineConstWithFallback;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

import java.util.List;

public class BoolType implements BuiltinType {

    public static final BoolType INSTANCE = new BoolType();
    private BoolType() {}

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef boolType = checker.getBasicBuiltin(INSTANCE);

        return ListUtils.join(List.of(
                //eq
                DefineConstWithFallback.<Boolean, Boolean, Boolean>defineBinary("eq", Boolean::equals, boolType, boolType, boolType, v -> {
                    v.visitInsn(Opcodes.IXOR);
                    v.visitInsn(Opcodes.ICONST_1);
                    v.visitInsn(Opcodes.IXOR);
                }),
                //not
                DefineConstWithFallback.<Boolean, Boolean>defineUnary("not", a -> !a, boolType, boolType, v -> {
                    v.visitInsn(Opcodes.ICONST_1);
                    v.visitInsn(Opcodes.IXOR);
                }),
                //truthy
                DefineConstWithFallback.defineUnary("bool", a -> a, boolType, boolType, v -> {
                    //Do nothing
                })
        ));
    }

    @Override
    public String name() {
        return "bool";
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return List.of("Z");
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return "Z";
    }

    @Override
    public boolean isReferenceType(TypeChecker checker, List<TypeDef> generics) {
        return false;
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

}
