package builtin_types.types;

import ast.passes.TypeChecker;
import ast.typed.def.method.BytecodeMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

public class ObjType implements BuiltinType {

    public static final ObjType INSTANCE = new ObjType();
    private ObjType() {}

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef thisType = checker.getBasicBuiltin(INSTANCE);
        TypeDef unitType = checker.getBasicBuiltin(UnitType.INSTANCE);
        return List.of(
                new BytecodeMethodDef("new", false, thisType, List.of(), unitType, false, v ->
                        v.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false))
        );
    }

    @Override
    public String name() {
        return "Obj";
    }

    @Override
    public String runtimeName(TypeChecker checker, List<TypeDef> generics) {
        return Type.getInternalName(Object.class);
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return List.of(Type.getDescriptor(Object.class));
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return Type.getDescriptor(Object.class);
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
        return true;
    }

    @Override
    public boolean hasSpecialConstructor(TypeChecker checker, List<TypeDef> generics) {
        return false;
//        return true;
    }

    @Override
    public int stackSlots(TypeChecker checker, List<TypeDef> generics) {
        return 1;
    }
}
