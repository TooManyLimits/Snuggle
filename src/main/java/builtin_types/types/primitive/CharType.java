package builtin_types.types.primitive;

import ast.passes.TypeChecker;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.helpers.DefineConstWithFallback;
import builtin_types.types.StringType;
import lexing.Loc;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

import java.math.BigInteger;
import java.util.List;

public class CharType extends IntegerType implements BuiltinType {

    public static final CharType INSTANCE = new CharType();
    private CharType() {
        super(false, 16);
    }

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef charType = checker.getBasicBuiltin(INSTANCE);
        TypeDef u32Type = checker.getBasicBuiltin(IntegerType.U32);
        TypeDef stringType = checker.getBasicBuiltin(StringType.INSTANCE);
        TypeDef boolType = checker.getBasicBuiltin(BoolType.INSTANCE);
        return ListUtils.join(
                //Checking properties of the char, delegates to certain java methods
                DefineConstWithFallback.defineUnary("isDigit", (Character c) -> Character.isDigit(c), charType, boolType,
                        v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "isDigit", "(C)Z", false)),
                DefineConstWithFallback.defineUnary("isWhitespace", (Character c) -> Character.isWhitespace(c), charType, boolType,
                        v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "isWhitespace", "(C)Z", false)),
                DefineConstWithFallback.defineUnary("isLetter", (Character c) -> Character.isDigit(c), charType, boolType,
                        v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "isLetter", "(C)Z", false)),
                DefineConstWithFallback.defineUnary("isUppercase", (Character c) -> Character.isUpperCase(c), charType, boolType,
                        v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "isUppercase", "(C)Z", false)),
                DefineConstWithFallback.defineUnary("isLowercase", (Character c) -> Character.isLowerCase(c), charType, boolType,
                        v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "isLowercase", "(C)Z", false)),

                //Converting chars
                DefineConstWithFallback.defineUnary("toUppercase", (Character c) -> Character.toUpperCase(c), charType, boolType,
                        v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "toUpperCase", "(C)C", false)),
                DefineConstWithFallback.defineUnary("toLowercase", (Character c) -> Character.toLowerCase(c), charType, boolType,
                        v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "toLowerCase", "(C)C", false)),

                //Char arithmetic
                //Takes advantage of helper functions used by u16, since chars are u16 in Java
                DefineConstWithFallback.defineBinary("add", (Character a, Character b) -> a + b, charType, charType, charType,
                        doOperationThenConvert(v -> v.visitInsn(Opcodes.IADD))),
                DefineConstWithFallback.defineBinary("sub", (Character a, Character b) -> a - b, charType, charType, charType,
                        doOperationThenConvert(v -> v.visitInsn(Opcodes.ISUB))),

                //Comparison
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("gt", (a, b) -> a.compareTo(b) > 0, charType, charType, boolType, intCompare(Opcodes.IF_ICMPGT)),
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("lt", (a, b) -> a.compareTo(b) < 0, charType, charType, boolType, intCompare(Opcodes.IF_ICMPLT)),
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("ge", (a, b) -> a.compareTo(b) >= 0, charType, charType, boolType, intCompare(Opcodes.IF_ICMPGE)),
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("le", (a, b) -> a.compareTo(b) <= 0, charType, charType, boolType, intCompare(Opcodes.IF_ICMPLE)),
                DefineConstWithFallback.<BigInteger, BigInteger, Boolean>defineBinary("eq", (a, b) -> a.compareTo(b) == 0, charType, charType, boolType, intCompare(Opcodes.IF_ICMPEQ)),

                //Hash (no-op)
                DefineConstWithFallback.defineUnary("hash", (Character c) -> BigInteger.valueOf((long) c), charType, u32Type, v -> {}),

                //str()
                DefineConstWithFallback.defineUnary("str", (Character c) -> String.valueOf(c), charType, stringType,
                        v -> v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(C)Ljava/lang/String;", false))
        );
    }

    @Override
    public String name() {
        return "char";
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return List.of("C");
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return "C";
    }

}
