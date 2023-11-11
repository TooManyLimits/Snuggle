package builtin_types.types.numbers;

import ast.passes.TypeChecker;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import builtin_types.BuiltinType;
import builtin_types.helpers.DefineConstWithFallback;
import builtin_types.types.BoolType;
import builtin_types.types.StringType;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.Fraction;
import util.ListUtils;
import util.throwing_interfaces.ThrowingConsumer;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FloatType implements BuiltinType {

    public final int bits;
    public final String name;
    public final String descriptor;

    private FloatType(int bits) {
        this.bits = bits;
        this.name = "f" + bits;
        descriptor = switch (bits) {
            case 32 -> "F";
            case 64 -> "D";
            default -> throw new IllegalStateException("Illegal float bits?????? bug in compiler, please report!");
        };
    }

    public static final FloatType F32 = new FloatType(32);
    public static final FloatType F64 = new FloatType(64);

    public static final List<FloatType> ALL_FLOAT_TYPES = List.of(F32, F64);

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef type = checker.getBasicBuiltin(this);
        TypeDef boolType = checker.getBasicBuiltin(BoolType.INSTANCE);
        TypeDef stringType = checker.getBasicBuiltin(StringType.INSTANCE);

        Function<Object, Float> floatConverter = x -> (x instanceof Fraction f) ? f.floatValue() : (Float) x;
        Function<Object, Double> doubleConverter = x -> (x instanceof Fraction f) ? f.doubleValue() : (Double) x;

        BinHelper binHelper = (name, floatFunc, doubleFunc, floatConsumer, doubleConsumer) -> {
            if (bits == 32)
                return DefineConstWithFallback.defineBinaryWithConverter(name, floatFunc, floatConverter, floatConverter, type, type, type, floatConsumer);
            return DefineConstWithFallback.defineBinaryWithConverter(name, doubleFunc, doubleConverter, doubleConverter, type, type, type, doubleConsumer);
        };

        CmpHelper cmpHelper = (name, floatFunc, doubleFunc, ifOp) -> {
            if (bits == 32)
                return DefineConstWithFallback.defineBinaryWithConverter(name, floatFunc, floatConverter, floatConverter, type, type, boolType, floatCompare(ifOp));
            return DefineConstWithFallback.defineBinaryWithConverter(name, doubleFunc, doubleConverter, doubleConverter, type, type, boolType, floatCompare(ifOp));
        };

        return ListUtils.join(List.of(
                //Arithmetic
                binHelper.get("add", (a, b) -> a+b, (a, b) -> a+b, v -> v.visitInsn(Opcodes.FADD), v -> v.visitInsn(Opcodes.DADD)),
                binHelper.get("sub", (a, b) -> a-b, (a, b) -> a-b, v -> v.visitInsn(Opcodes.FSUB), v -> v.visitInsn(Opcodes.DSUB)),
                binHelper.get("mul", (a, b) -> a*b, (a, b) -> a*b, v -> v.visitInsn(Opcodes.FMUL), v -> v.visitInsn(Opcodes.DMUL)),
                binHelper.get("div", (a, b) -> a/b, (a, b) -> a/b, v -> v.visitInsn(Opcodes.FDIV), v -> v.visitInsn(Opcodes.DDIV)),
                binHelper.get("rem", (a, b) -> a%b, (a, b) -> a%b, v -> v.visitInsn(Opcodes.FREM), v -> v.visitInsn(Opcodes.DREM)),

                //Unary ops
                bits == 32 ? //neg
                    DefineConstWithFallback.defineUnary("neg", (Float f) -> -f, type, type, v -> v.visitInsn(Opcodes.FNEG)) :
                    DefineConstWithFallback.defineUnary("neg", (Double d) -> -d, type, type, v -> v.visitInsn(Opcodes.DNEG)),
                bits == 32 ? //sqrt
                        DefineConstWithFallback.defineUnary("sqrt", (Float f) -> (float) Math.sqrt(f), type, type, v -> {
                            v.visitInsn(Opcodes.F2D);
                            v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sqrt", "(D)D", false);
                            v.visitInsn(Opcodes.D2F);
                        }) :
                        DefineConstWithFallback.defineUnary("sqrt", Math::sqrt, type, type, v ->
                                v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sqrt", "(D)D", false)),

                //Comparisons
                cmpHelper.get("gt", (a, b) -> a>b, (a, b) -> a>b, Opcodes.IFLE),
                cmpHelper.get("lt", (a, b) -> a<b, (a, b) -> a<b, Opcodes.IFGE),
                cmpHelper.get("ge", (a, b) -> a>=b, (a, b) -> a>=b, Opcodes.IFLT),
                cmpHelper.get("le", (a, b) -> a<=b, (a, b) -> a<=b, Opcodes.IFGT),
                cmpHelper.get("eq", Float::equals, Double::equals, Opcodes.IFNE),

                //Other
                DefineConstWithFallback.defineUnary("str", Object::toString, type, stringType, v ->
                        v.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(" + descriptor + ")Ljava/lang/String;", false))
        ));

    }

    //the 2 args are on the stack.
    //ifOp should be the "opposite" of what you actually want:
    //for the less than operator, we want GE.
    //for the less-equal operator, we want GT. Etc
    private ThrowingConsumer<MethodVisitor, CompilationException> floatCompare(int ifOp) {
        return v -> {
            Label pushFalse = new Label();
            Label end = new Label();

            int floatComparisonOp = switch (ifOp) {
                case Opcodes.IFLT, Opcodes.IFLE, Opcodes.IFNE -> bits == 32 ? Opcodes.FCMPG : Opcodes.DCMPG;
                case Opcodes.IFGT, Opcodes.IFGE -> bits == 32 ? Opcodes.FCMPL : Opcodes.DCMPL;
                default -> throw new IllegalStateException("Unexpected compare op: bug in compiler, please report!");
            };

            //Perform the float comparison op. Because of the above switch statement,
            //in the case of NaN, something which will cause a jump to "pushFalse" is pushed.
            v.visitInsn(floatComparisonOp);
            v.visitJumpInsn(ifOp, pushFalse);
            v.visitInsn(Opcodes.ICONST_1);
            v.visitJumpInsn(Opcodes.GOTO, end);
            v.visitLabel(pushFalse);
            v.visitInsn(Opcodes.ICONST_0);
            v.visitLabel(end);
        };
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> descriptor(TypeChecker checker, List<TypeDef> generics) {
        return List.of(descriptor);
    }

    @Override
    public String returnDescriptor(TypeChecker checker, List<TypeDef> generics) {
        return descriptor;
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
        return bits == 64 ? 2 : 1;
    }

    @FunctionalInterface
    private interface BinHelper {
        List<MethodDef> get(String name,
                            BiFunction<Float, Float, Float> floatFunc,
                            BiFunction<Double, Double, Double> doubleFunc,
                            ThrowingConsumer<MethodVisitor, CompilationException> floatConsumer,
                            ThrowingConsumer<MethodVisitor, CompilationException> doubleConsumer
        );
    }

    @FunctionalInterface
    private interface CmpHelper {
        List<MethodDef> get(String name,
                            BiFunction<Float, Float, Boolean> floatFunc,
                            BiFunction<Double, Double, Boolean> doubleFunc,
                            int ifOp
        );
    }

}
