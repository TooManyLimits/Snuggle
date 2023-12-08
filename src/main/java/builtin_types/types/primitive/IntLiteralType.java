package builtin_types.types.primitive;

import ast.passes.TypeChecker;
import ast.typed.def.method.ConstMethodDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedMethodCall;
import exceptions.compile_time.CompilationException;
import ast.typed.def.method.MethodDef;
import ast.typed.expr.TypedLiteral;
import builtin_types.BuiltinType;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.Fraction;
import util.ListUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class IntLiteralType implements BuiltinType {

    public static final IntLiteralType INSTANCE = new IntLiteralType();
    private IntLiteralType() {}

    /**
     * Generate a binary ConstMethodDef with the given name and func
     *
     * Generic is the return type. This method can be repurposed to work with, say,
     * comparison operators, by replacing T = BigInteger with T = Boolean.
     */
    private <T1, T2, T3, T4> MethodDef generateBinary(String name,
                                         BiFunction<BigInteger, BigInteger, T1> intFunc,
                                         BiFunction<BigInteger, Fraction, T2> fractionFunc,
                                         BiFunction<BigInteger, Float, T3> floatFunc,
                                         BiFunction<BigInteger, Double, T4> doubleFunc,
                                         TypeDef intLiteralType,
                                         BuiltinType unmappedArgType, TypeDef mappedArgType,
                                         TypeDef returnType, TypeChecker checker) {
        return new ConstMethodDef(name, 0, false, List.of(mappedArgType), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral typedReceiver && typedReceiver.type().equals(intLiteralType)) {
                //Get the receiver value
                BigInteger receiverValue = (BigInteger) typedReceiver.obj();
                if (call.args().get(0) instanceof TypedLiteral literalArg) {
                    //If the arg is also an literal, we can merge these. Example: 1 + 3
                    if (literalArg.obj() instanceof BigInteger argValue)
                        return new TypedLiteral(typedReceiver.cause(), call.loc(), intFunc.apply(receiverValue, argValue), returnType);
                    if (literalArg.obj() instanceof Fraction argValue)
                        return new TypedLiteral(typedReceiver.cause(), call.loc(), fractionFunc.apply(receiverValue, argValue), returnType);
                    if (literalArg.obj() instanceof Float argValue)
                        return new TypedLiteral(typedReceiver.cause(), call.loc(), floatFunc.apply(receiverValue, argValue), returnType);
                    if (literalArg.obj() instanceof Double argValue)
                        return new TypedLiteral(typedReceiver.cause(), call.loc(), doubleFunc.apply(receiverValue, argValue), returnType);
                    throw new IllegalStateException("Unexpected arg type to int literal function - " + literalArg.obj());
                } else if (mappedArgType == intLiteralType) {
                    throw new IllegalStateException("Calling int literal method expecting literal arg, on non-literal arg? Bug in compiler, please report!");
                } else {
                    //If the arg is not an int literal, we can't merge at compile time. Example: 1 + x.
                    //Find the correct method def, with the proper name and arg type
                    String desiredName = "n_" + name;
                    MethodDef neededMethodDef = ListUtils.find(
                            checker.getBasicBuiltin(unmappedArgType).methods(),
                            m -> m.name().equals(desiredName) //&& m.paramTypeGetter().get(0).equals(mappedArgType)
                    );
                    //And return a new typed call using it.
                    return new TypedMethodCall(call.loc(), typedReceiver.pullTypeUpwards(mappedArgType), neededMethodDef, call.args(), returnType);
                }
            } else {
                throw new IllegalStateException("Calling int literal method on non-int-literal? Bug in compiler, please report");
            }
        }, null, null);
    }

    //Generate many binary methods
    private List<MethodDef> generateBinary(String name,
                                           BiFunction<BigInteger, BigInteger, BigInteger> intFunc,
                                           BiFunction<BigInteger, Fraction, Fraction> fractionFunc,
                                           BiFunction<BigInteger, Float, Float> floatFunc,
                                           BiFunction<BigInteger, Double, Double> doubleFunc,
                                           TypeDef mappedIntLiteralType, TypeDef mappedFloatLiteralType,
                                           List<TypeDef> mappedIntTypes, List<TypeDef> mappedFloatTypes,
                                           TypeChecker checker) {
        ArrayList<MethodDef> list = new ArrayList<>();
        //IntLiteral, IntLiteral -> IntLiteral
        list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, null, mappedIntLiteralType, mappedIntLiteralType, checker));
        //IntLiteral, FloatLiteral -> FloatLiteral
        list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, null, mappedFloatLiteralType, mappedFloatLiteralType, checker));

        for (int i = 0; i < mappedIntTypes.size(); i++) {
            IntegerType unmappedIntType = IntegerType.ALL_INT_TYPES.get(i);
            TypeDef mappedIntType = mappedIntTypes.get(i);
            //IntLiteral, IntLiteral -> Concrete
            list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, null, mappedIntLiteralType, mappedIntType, checker));
            //IntLiteral, Concrete Int -> Concrete
            list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, unmappedIntType, mappedIntType, mappedIntType, checker));
        }
        for (int i = 0; i < mappedFloatTypes.size(); i++) {
            FloatType unmappedFloatType = FloatType.ALL_FLOAT_TYPES.get(i);
            TypeDef mappedFloatType = mappedFloatTypes.get(i);
            //IntLiteral, FloatLiteral -> Concrete
            list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, null, mappedFloatLiteralType, mappedFloatType, checker));
            //IntLiteral, Concrete Float -> Concrete
            list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, unmappedFloatType, mappedFloatType, mappedFloatType, checker));
        }

        list.trimToSize();
        return list;
    }

    //Generate many binary methods
    private List<MethodDef> generateBinaryIntOnly(String name,
                                           BiFunction<BigInteger, BigInteger, BigInteger> intFunc,
                                           TypeDef mappedIntLiteralType,
                                           List<TypeDef> mappedIntTypes,
                                           TypeChecker checker) {
        ArrayList<MethodDef> list = new ArrayList<>();
        //IntLiteral, IntLiteral -> IntLiteral
        list.add(generateBinary(name, intFunc, null, null, null, mappedIntLiteralType, null, mappedIntLiteralType, mappedIntLiteralType, checker));
        for (int i = 0; i < mappedIntTypes.size(); i++) {
            IntegerType unmappedIntType = IntegerType.ALL_INT_TYPES.get(i);
            TypeDef mappedIntType = mappedIntTypes.get(i);
            //IntLiteral, IntLiteral -> Concrete
            list.add(generateBinary(name, intFunc, null, null, null, mappedIntLiteralType, null, mappedIntLiteralType, mappedIntType, checker));
            //IntLiteral, Concrete Int -> Concrete
            list.add(generateBinary(name, intFunc, null, null, null, mappedIntLiteralType, unmappedIntType, mappedIntType, mappedIntType, checker));
        }
        list.trimToSize();
        return list;
    }


    //Generate many comparison methods
    private List<MethodDef> generateComparison(String name,
                                               BiFunction<BigInteger, BigInteger, Boolean> intFunc,
                                               BiFunction<BigInteger, Fraction, Boolean> fractionFunc,
                                               BiFunction<BigInteger, Float, Boolean> floatFunc,
                                               BiFunction<BigInteger, Double, Boolean> doubleFunc,
                                               TypeDef mappedIntLiteralType, TypeDef mappedFloatLiteralType,
                                               List<TypeDef> mappedIntTypes, List<TypeDef> mappedFloatTypes,
                                               TypeDef mappedBoolType,
                                               TypeChecker checker) {
        ArrayList<MethodDef> list = new ArrayList<>();
        //IntLiteral, IntLiteral -> Boolean
        list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, null, mappedIntLiteralType, mappedBoolType, checker));
        //IntLiteral, FloatLiteral -> Boolean
        list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, null, mappedFloatLiteralType, mappedBoolType, checker));

        for (int i = 0; i < mappedIntTypes.size(); i++) {
            IntegerType unmappedIntType = IntegerType.ALL_INT_TYPES.get(i);
            TypeDef mappedIntType = mappedIntTypes.get(i);
            //IntLiteral, Concrete Int -> Boolean
            list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, unmappedIntType, mappedIntType, mappedBoolType, checker));
        }
        for (int i = 0; i < mappedFloatTypes.size(); i++) {
            FloatType unmappedFloatType = FloatType.ALL_FLOAT_TYPES.get(i);
            TypeDef mappedFloatType = mappedFloatTypes.get(i);
            //IntLiteral, Concrete Float -> Boolean
            list.add(generateBinary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, unmappedFloatType, mappedFloatType, mappedBoolType, checker));
        }
        list.trimToSize();
        return list;
    }

    /**
     * Generate a unary ConstMethodDef with the given name and func
     */
    private <T> MethodDef generateUnary(String name,
                                        Function<BigInteger, BigInteger> intFunc,
                                        Function<BigInteger, Fraction> fractionFunc,
                                        Function<BigInteger, Float> floatFunc,
                                        Function<BigInteger, Double> doubleFunc,
                                        TypeDef intLiteralType,
                                        TypeDef returnType) {
        return new ConstMethodDef(name, 0, false, List.of(), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral typedReceiver && typedReceiver.type().equals(intLiteralType)) {
                //Get the receiver value
                BigInteger receiverValue = (BigInteger) typedReceiver.obj();

                //"Switch" on return type
                if (call.type().builtin() == IntLiteralType.INSTANCE || call.type().builtin() instanceof IntegerType)
                    return new TypedLiteral(typedReceiver.cause(), call.loc(), intFunc.apply(receiverValue), call.type());
                if (call.type().builtin() == FloatLiteralType.INSTANCE)
                    return new TypedLiteral(typedReceiver.cause(), call.loc(), fractionFunc.apply(receiverValue), call.type());
                if (call.type().builtin() == FloatType.F32)
                    return new TypedLiteral(typedReceiver.cause(), call.loc(), floatFunc.apply(receiverValue), call.type());
                if (call.type().builtin() == FloatType.F64)
                    return new TypedLiteral(typedReceiver.cause(), call.loc(), doubleFunc.apply(receiverValue), call.type());
                throw new IllegalStateException("Unexpected return type for int literal function - " + call.type());
            } else {
                throw new IllegalStateException("Calling int literal method on non-int-literal? Bug in compiler, please report");
            }
        }, null, null);
    }

    private <T> List<MethodDef> generateUnary(String name,
                                              Function<BigInteger, BigInteger> intFunc,
                                              Function<BigInteger, Fraction> fractionFunc,
                                              Function<BigInteger, Float> floatFunc,
                                              Function<BigInteger, Double> doubleFunc,
                                              TypeDef mappedIntLiteralType, TypeDef mappedFloatLiteralType,
                                              List<TypeDef> mappedIntTypes, List<TypeDef> mappedFloatTypes) {
        ArrayList<MethodDef> list = new ArrayList<>();
        //IntLiteral -> IntLiteral
        list.add(generateUnary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, mappedIntLiteralType));
        //IntLiteral -> FloatLiteral
        list.add(generateUnary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, mappedFloatLiteralType));

        for (TypeDef mappedIntType : mappedIntTypes)
            //IntLiteral -> Concrete Int
            list.add(generateUnary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, mappedIntType));
        for (TypeDef mappedFloatType : mappedFloatTypes)
            //IntLiteral -> Concrete Float
            list.add(generateUnary(name, intFunc, fractionFunc, floatFunc, doubleFunc, mappedIntLiteralType, mappedFloatType));

        list.trimToSize();
        return list;
    }

    private <T> List<MethodDef> generateUnaryIntOnly(String name, Function<BigInteger, BigInteger> intFunc, TypeDef mappedIntLiteralType, List<TypeDef> mappedIntTypes) {
        ArrayList<MethodDef> list = new ArrayList<>();
        //IntLiteral -> IntLiteral
        list.add(generateUnary(name, intFunc, null, null, null, mappedIntLiteralType, mappedIntLiteralType));
        for (TypeDef mappedIntType : mappedIntTypes)
            //IntLiteral -> Concrete Int
            list.add(generateUnary(name, intFunc, null, null, null, mappedIntLiteralType, mappedIntType));
        list.trimToSize();
        return list;
    }

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics, Loc instantiationLoc, TypeDef.InstantiationStackFrame cause) {
        TypeDef mappedIntLiteralType = checker.getBasicBuiltin(INSTANCE);
        TypeDef mappedFloatLiteralType = checker.getBasicBuiltin(FloatLiteralType.INSTANCE);
        TypeDef mappedBoolType = checker.getBasicBuiltin(BoolType.INSTANCE);
        List<TypeDef> mappedIntTypes = ListUtils.map(IntegerType.ALL_INT_TYPES, checker::getBasicBuiltin);
        List<TypeDef> mappedFloatTypes = ListUtils.map(FloatType.ALL_FLOAT_TYPES, checker::getBasicBuiltin);
        return ListUtils.join(List.of(
                //Arithmetic
                generateBinary("add",
                        BigInteger::add,
                        (a, b) -> new Fraction(a, BigInteger.ONE).add(b),
                        (a, b) -> a.floatValue() + b,
                        (a, b) -> a.doubleValue() + b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, checker),
                generateBinary("sub",
                        BigInteger::subtract,
                        (a, b) -> new Fraction(a, BigInteger.ONE).subtract(b),
                        (a, b) -> a.floatValue() - b,
                        (a, b) -> a.doubleValue() - b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, checker),
                generateBinary("mul",
                        BigInteger::multiply,
                        (a, b) -> new Fraction(a, BigInteger.ONE).multiply(b),
                        (a, b) -> a.floatValue() * b,
                        (a, b) -> a.doubleValue() * b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, checker),
                generateBinary("div",
                        BigInteger::divide,
                        (a, b) -> new Fraction(a, BigInteger.ONE).divide(b),
                        (a, b) -> a.floatValue() / b,
                        (a, b) -> a.doubleValue() / b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, checker),
                generateBinary("rem",
                        BigInteger::remainder,
                        (a, b) -> new Fraction(a, BigInteger.ONE).remainder(b),
                        (a, b) -> a.floatValue() % b,
                        (a, b) -> a.doubleValue() % b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, checker),

                //Bitwise
                generateBinaryIntOnly("band", BigInteger::and, mappedIntLiteralType, mappedIntTypes, checker),
                generateBinaryIntOnly("bor", BigInteger::or, mappedIntLiteralType, mappedIntTypes, checker),
                generateBinaryIntOnly("bxor", BigInteger::xor, mappedIntLiteralType, mappedIntTypes, checker),

                generateBinaryIntOnly("shl", (a, b) -> a.shiftLeft(b.intValue()), mappedIntLiteralType, mappedIntTypes, checker),
                generateBinaryIntOnly("shr", (a, b) -> a.shiftRight(b.intValue()), mappedIntLiteralType, mappedIntTypes, checker),

                //Comparison
                generateComparison("gt",
                        (a, b) -> a.compareTo(b) > 0,
                        (a, b) -> new Fraction(a, BigInteger.ONE).compareTo(b) > 0,
                        (a, b) -> a.floatValue() > b,
                        (a, b) -> a.doubleValue() > b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, mappedBoolType, checker),
                generateComparison("lt",
                        (a, b) -> a.compareTo(b) < 0,
                        (a, b) -> new Fraction(a, BigInteger.ONE).compareTo(b) < 0,
                        (a, b) -> a.floatValue() < b,
                        (a, b) -> a.doubleValue() < b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, mappedBoolType, checker),
                generateComparison("ge",
                        (a, b) -> a.compareTo(b) >= 0,
                        (a, b) -> new Fraction(a, BigInteger.ONE).compareTo(b) >= 0,
                        (a, b) -> a.floatValue() >= b,
                        (a, b) -> a.doubleValue() >= b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, mappedBoolType, checker),
                generateComparison("le",
                        (a, b) -> a.compareTo(b) <= 0,
                        (a, b) -> new Fraction(a, BigInteger.ONE).compareTo(b) <= 0,
                        (a, b) -> a.floatValue() <= b,
                        (a, b) -> a.doubleValue() <= b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, mappedBoolType, checker),
                generateComparison("eq",
                        (a, b) -> a.compareTo(b) == 0,
                        (a, b) -> new Fraction(a, BigInteger.ONE).compareTo(b) == 0,
                        (a, b) -> a.floatValue() == b,
                        (a, b) -> a.doubleValue() == b,
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes, mappedBoolType, checker),

                //Unary
                generateUnary("neg",
                        BigInteger::negate,
                        i -> new Fraction(i, BigInteger.ONE).negate(),
                        i -> -i.floatValue(),
                        i -> -i.doubleValue(),
                        mappedIntLiteralType, mappedFloatLiteralType, mappedIntTypes, mappedFloatTypes),
                generateUnaryIntOnly("bnot", BigInteger::not, mappedIntLiteralType, mappedIntTypes)
        ));
    }

    @Override
    public String name() {
        return "IntLiteral";
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
        return Set.copyOf(ListUtils.map(IntegerType.ALL_INT_TYPES, checker::getBasicBuiltin));
    }

    @Override
    public TypeDef compileTimeToRuntimeConvert(TypeDef thisType, Loc loc, TypeDef.InstantiationStackFrame cause, TypeChecker checker) throws CompilationException {
        throw new TypeCheckingException("Unable to determine type of int literal. Try adding annotations!", loc, cause);
    }
}
