package builtin_types.types;

import ast.passes.TypeChecker;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.helpers.DefineConstWithFallback;
import builtin_types.types.primitive.BoolType;
import builtin_types.types.primitive.CharType;
import builtin_types.types.primitive.IntegerType;
import lexing.Loc;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import util.ListUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

public class StringType implements BuiltinType {

    public static final StringType INSTANCE = new StringType();
    private StringType() {}

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef stringType = checker.getBasicBuiltin(INSTANCE);
        TypeDef charType = checker.getBasicBuiltin(CharType.INSTANCE);
        TypeDef u32Type = checker.getBasicBuiltin(IntegerType.U32);
        TypeDef boolType = checker.getBasicBuiltin(BoolType.INSTANCE);
        return ListUtils.join(
                //Add two strings together to concat
                DefineConstWithFallback.defineBinary("add", String::concat, stringType, stringType, stringType,
                        v -> v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false)),
                //Length of string
                DefineConstWithFallback.defineUnary("size", (String x) -> BigInteger.valueOf(x.length()), stringType, u32Type,
                        v -> v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)),
                //Equality of strings
                DefineConstWithFallback.defineBinary("eq", (String x, String y) -> x.equals(y), stringType, stringType, boolType,
                        v -> v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false)),
                //Get char at given index
                DefineConstWithFallback.defineBinary("get", String::charAt, stringType, u32Type, charType,
                        v -> v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false))
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
    public Set<TypeDef> getTypeCheckingSupertypes(TypeChecker checker, List<TypeDef> generics) {
        return Set.of(checker.getBasicBuiltin(ObjType.INSTANCE));
    }

    @Override
    public TypeDef getInheritanceSupertype(TypeChecker checker, List<TypeDef> generics) {
        return checker.getBasicBuiltin(ObjType.INSTANCE);
    }
}
