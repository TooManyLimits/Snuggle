package builtin_types.types.numbers;

import ast.passes.TypeChecker;
import ast.typed.def.method.ConstMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedMethodCall;
import builtin_types.BuiltinType;
import builtin_types.types.BoolType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.Fraction;
import util.ListUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Class is heavily based on IntLiteralType, just with some things changed around
 * to work with floating point instead.
 */
public class FloatLiteralType implements BuiltinType {

    public static final FloatLiteralType INSTANCE = new FloatLiteralType();
    private FloatLiteralType() {}

    /**
     * Generate a binary ConstMethodDef with the given name and func
     *
     * Generic is the return type.
     * This is essentially modeled the same as the IntLiteralType version of the function.
     */
    private <T1, T2> MethodDef generateBinary(String name,
                                                   BiFunction<Fraction, T1, T2> func,
                                                   TypeDef floatLiteralType, FloatType unmappedArgType,
                                                   TypeDef argType, TypeDef returnType,
                                                   TypeChecker checker) {

        return new ConstMethodDef(name, 0, false, List.of(argType), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral typedReceiver && typedReceiver.type().equals(floatLiteralType)) {
                //Get the receiver value
                Fraction receiverValue = (Fraction) typedReceiver.obj();
                //Test it against the 3 possible floating point types (float, double, fraction)
                if (call.args().get(0) instanceof TypedLiteral literalArg) {
                    Object literalValue = literalArg.obj();
                    T2 resultValue = func.apply(receiverValue, (T1) literalValue);
                    return new TypedLiteral(call.loc(), resultValue, returnType);
                } else if (argType == floatLiteralType) {
                    throw new IllegalStateException("Calling float literal method expecting literal arg, on non-literal arg? Bug in compiler, please report!");
                } else {
                    //If the arg is not a literal, we can't merge at compile time. Example: 1 + x.
                    //Find the correct method def, with the proper name and arg type
                    String desiredName = "n_" + name;
                    MethodDef neededMethodDef = ListUtils.find(
                            checker.getBasicBuiltin(unmappedArgType).methods(),
                            m -> m.name().equals(desiredName) && m.paramTypes().get(0).equals(argType)
                    );
                    //And return a new typed call using it.
                    return new TypedMethodCall(call.loc(), typedReceiver.pullTypeUpwards(argType), neededMethodDef, call.args(), returnType);
                }
            } else {
                throw new IllegalStateException("Calling float literal method on non-float-literal? Bug in compiler, please report");
            }
        }, null);
    }

    //Generate many binary methods
    private List<MethodDef> generateBinary(String name,
                                                BiFunction<Fraction, Float, Float> floatFunc,
                                                BiFunction<Fraction, Double, Double> doubleFunc,
                                                BiFunction<Fraction, Fraction, Fraction> fractionFunc,
                                                TypeDef mappedFloatLiteralType, TypeDef mappedF32Type, TypeDef mappedF64Type,
                                                TypeChecker checker) {
        return List.of(
                //Literal, Literal -> Literal
                generateBinary(name, fractionFunc, mappedFloatLiteralType, null, mappedFloatLiteralType, mappedFloatLiteralType, checker),
                //Literal, f32 -> f32
                generateBinary(name, floatFunc, mappedFloatLiteralType, FloatType.F32, mappedF32Type, mappedF32Type, checker),
                //Literal, f64 -> f64
                generateBinary(name, doubleFunc, mappedFloatLiteralType, FloatType.F64, mappedF64Type, mappedF64Type, checker)
        );
    }

    //Generate many comparison methods
    private List<MethodDef> generateComparison(String name,
                                                    BiFunction<Fraction, Float, Boolean> floatFunc,
                                                    BiFunction<Fraction, Double, Boolean> doubleFunc,
                                                    BiFunction<Fraction, Fraction, Boolean> fractionFunc,
                                                    TypeDef mappedFloatLiteralType, TypeDef mappedBoolType, TypeDef mappedF32Type, TypeDef mappedF64Type,
                                                    TypeChecker checker) {
        return List.of(
                //Literal, Literal -> bool
                generateBinary(name, fractionFunc, mappedFloatLiteralType, null, mappedFloatLiteralType, mappedBoolType, checker),
                //Literal, f32 -> bool
                generateBinary(name, floatFunc, mappedFloatLiteralType, FloatType.F32, mappedF32Type, mappedBoolType, checker),
                //Literal, f64 -> bool
                generateBinary(name, doubleFunc, mappedFloatLiteralType, FloatType.F64, mappedF64Type, mappedBoolType, checker)
        );
    }

    /**
     * Generate a unary ConstMethodDef with the given name and funcs
     */
    private <T> MethodDef generateUnary(String name, Function<Fraction, T> func, TypeDef floatLiteralType, TypeDef returnType) {
        return new ConstMethodDef(name, 0, false, List.of(), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral typedReceiver && typedReceiver.type().equals(floatLiteralType)) {
                //Get the receiver value and apply the function
                Fraction receiverValue = (Fraction) typedReceiver.obj();
                return new TypedLiteral(call.loc(), func.apply(receiverValue), returnType);
            } else {
                throw new IllegalStateException("Calling int literal method on non-int-literal? Bug in compiler, please report");
            }
        }, null);
    }

    //Generate multiple unary methods
    private List<MethodDef> generateUnary(String name,
                                               Function<Fraction, Float> floatFunc,
                                               Function<Fraction, Double> doubleFunc,
                                               Function<Fraction, Fraction> fractionFunc,
                                               TypeDef mappedFloatLiteralType,
                                               TypeDef mappedF32Type,
                                               TypeDef mappedF64Type) {
        return List.of(
                //Literal -> Literal
                generateUnary(name, fractionFunc, mappedFloatLiteralType, mappedFloatLiteralType),
                //Literal -> f32
                generateUnary(name, floatFunc, mappedFloatLiteralType, mappedF32Type),
                //Literal -> f64
                generateUnary(name, doubleFunc, mappedFloatLiteralType, mappedF64Type)
        );
    }


    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics) {
        TypeDef mappedFloatLiteralType = checker.getBasicBuiltin(INSTANCE);
        TypeDef mappedF32Type = checker.getBasicBuiltin(FloatType.F32);
        TypeDef mappedF64Type = checker.getBasicBuiltin(FloatType.F64);
        TypeDef mappedBoolType = checker.getBasicBuiltin(BoolType.INSTANCE);

        BinHelper binHelper = (name, f1, f2, f3) -> generateBinary(name, f1, f2, f3, mappedFloatLiteralType, mappedF32Type, mappedF64Type, checker);
        CmpHelper cmpHelper = (name, f1, f2, f3) -> generateComparison(name, f1, f2, f3, mappedFloatLiteralType, mappedBoolType, mappedF32Type, mappedF64Type, checker);

        return ListUtils.join(List.of(
                binHelper.get("add", (frac, f) -> frac.floatValue() + f, (frac, d) -> frac.doubleValue() + d, Fraction::add),
                binHelper.get("sub", (frac, f) -> frac.floatValue() - f, (frac, d) -> frac.doubleValue() - d, Fraction::subtract),
                binHelper.get("mul", (frac, f) -> frac.floatValue() * f, (frac, d) -> frac.doubleValue() * d, Fraction::multiply),
                binHelper.get("div", (frac, f) -> frac.floatValue() / f, (frac, d) -> frac.doubleValue() / d, Fraction::divide),
                binHelper.get("rem", (frac, f) -> frac.floatValue() % f, (frac, d) -> frac.doubleValue() % d, Fraction::remainder),

                cmpHelper.get("gt", (frac, f) -> frac.floatValue() > f, (frac, d) -> frac.doubleValue() > d, (frac1, frac2) -> frac1.compareTo(frac2) > 0),
                cmpHelper.get("lt", (frac, f) -> frac.floatValue() < f, (frac, d) -> frac.doubleValue() < d, (frac1, frac2) -> frac1.compareTo(frac2) < 0),
                cmpHelper.get("ge", (frac, f) -> frac.floatValue() >= f, (frac, d) -> frac.doubleValue() >= d, (frac1, frac2) -> frac1.compareTo(frac2) >= 0),
                cmpHelper.get("le", (frac, f) -> frac.floatValue() <= f, (frac, d) -> frac.doubleValue() <= d, (frac1, frac2) -> frac1.compareTo(frac2) <= 0),
                cmpHelper.get("eq", (frac, f) -> frac.floatValue() == f, (frac, d) -> frac.doubleValue() == d, (frac1, frac2) -> frac1.compareTo(frac2) == 0),

                generateUnary("neg", f -> -f.floatValue(), f -> -f.doubleValue(), Fraction::negate, mappedFloatLiteralType, mappedF32Type, mappedF64Type),
                List.of(generateUnary("not", f -> f.numerator.equals(BigInteger.ZERO), mappedFloatLiteralType, mappedBoolType))
        ));
    }

    @Override
    public String name() {
        return "FloatLiteral";
    }

    @Override
    public boolean nameable() {
        return false;
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
        return -1;
    }

    @Override
    public Set<TypeDef> getTypeCheckingSupertypes(TypeChecker checker, List<TypeDef> generics) {
        return Set.copyOf(ListUtils.map(FloatType.ALL_FLOAT_TYPES, checker::getBasicBuiltin));
    }

    @Override
    public TypeDef compileTimeToRuntimeConvert(TypeDef thisType, Loc loc, TypeChecker checker) throws CompilationException {
        throw new TypeCheckingException("Unable to determine type of float literal. Try adding annotations!", loc);
    }

    //Helpers for cleaner syntax in getMethods()
    @FunctionalInterface
    private interface BinHelper {
        List<MethodDef> get(String name,
                                 BiFunction<Fraction, Float, Float> floatFunc,
                                 BiFunction<Fraction, Double, Double> doubleFunc,
                                 BiFunction<Fraction, Fraction, Fraction> fractionFunc);
    }

    @FunctionalInterface
    private interface CmpHelper {
        List<MethodDef> get(String name,
                                 BiFunction<Fraction, Float, Boolean> floatFunc,
                                 BiFunction<Fraction, Double, Boolean> doubleFunc,
                                 BiFunction<Fraction, Fraction, Boolean> fractionFunc);
    }

}
