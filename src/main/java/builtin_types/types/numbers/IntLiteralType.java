package builtin_types.types.numbers;

import ast.passes.TypeChecker;
import ast.typed.def.method.ConstMethodDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedMethodCall;
import builtin_types.types.BoolType;
import exceptions.compile_time.CompilationException;
import ast.typed.def.method.MethodDef;
import ast.typed.expr.TypedLiteral;
import builtin_types.BuiltinType;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
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
    private <T> MethodDef generateBinary(String name, BiFunction<BigInteger, BigInteger, T> func, TypeDef intLiteralType, IntegerType unmappedArgType, TypeDef argType, TypeDef returnType, TypeChecker checker) {
        return new ConstMethodDef(name, 0, false, List.of(argType), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral typedReceiver && typedReceiver.type().equals(intLiteralType)) {
                //Get the receiver value
                BigInteger receiverValue = (BigInteger) typedReceiver.obj();
                if (call.args().get(0) instanceof TypedLiteral literalArg) {
                    //If the arg is also an int literal, we can merge these. Example: 1 + 3
                    BigInteger argValue = (BigInteger) literalArg.obj();
                    T resultValue = func.apply(receiverValue, argValue);
                    return new TypedLiteral(call.loc(), resultValue, returnType);
                } else if (argType == intLiteralType) {
                    throw new IllegalStateException("Calling int literal method expecting literal arg, on non-literal arg? Bug in compiler, please report!");
                } else {
                    //If the arg is not an int literal, we can't merge at compile time. Example: 1 + x.
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
                throw new IllegalStateException("Calling int literal method on non-int-literal? Bug in compiler, please report");
            }
        }, null);
    }

    //Generate many binary methods
    private List<MethodDef> generateBinary(String name, BiFunction<BigInteger, BigInteger, BigInteger> func, TypeDef mappedIntLiteralType, List<TypeDef> mappedIntTypes, TypeChecker checker) {
        ArrayList<MethodDef> list = new ArrayList<>();
        //Literal, Literal -> Literal
        list.add(generateBinary(name, func, mappedIntLiteralType, null, mappedIntLiteralType, mappedIntLiteralType, checker));
        for (int i = 0; i < mappedIntTypes.size(); i++) {
            IntegerType unmappedIntType = IntegerType.ALL_INT_TYPES.get(i);
            TypeDef mappedIntType = mappedIntTypes.get(i);
            //Literal, Literal -> Concrete
            list.add(generateBinary(name, func, mappedIntLiteralType, null, mappedIntLiteralType, mappedIntType, checker));
            //Literal, Concrete -> Concrete
            list.add(generateBinary(name, func, mappedIntLiteralType, unmappedIntType, mappedIntType, mappedIntType, checker));
        }
        list.trimToSize();
        return list;
    }

    //Generate many comparison methods
    private List<MethodDef> generateComparison(String name, BiFunction<BigInteger, BigInteger, Boolean> func, TypeDef mappedIntLiteralType, TypeDef mappedBoolType, List<TypeDef> mappedIntTypes, TypeChecker checker) {
        ArrayList<MethodDef> list = new ArrayList<>();
        //Literal, Literal -> Boolean
        list.add(generateBinary(name, func, mappedIntLiteralType, null, mappedIntLiteralType, mappedBoolType, checker));
        for (int i = 0; i < mappedIntTypes.size(); i++) {
            IntegerType unmappedIntType = IntegerType.ALL_INT_TYPES.get(i);
            TypeDef mappedIntType = mappedIntTypes.get(i);
            //Literal, Concrete -> Boolean
            list.add(generateBinary(name, func, mappedIntLiteralType, unmappedIntType, mappedIntType, mappedBoolType, checker));
        }
        list.trimToSize();
        return list;
    }

    /**
     * Generate a unary ConstMethodDef with the given name and func
     */
    private <T> MethodDef generateUnary(String name, Function<BigInteger, T> func, TypeDef intLiteralType, TypeDef returnType) {
        return new ConstMethodDef(name, 0, false, List.of(), returnType, call -> {
            if (call.receiver() instanceof TypedLiteral typedReceiver && typedReceiver.type().equals(intLiteralType)) {
                //Get the receiver value
                BigInteger receiverValue = (BigInteger) typedReceiver.obj();
                T resultValue = func.apply(receiverValue);
                return new TypedLiteral(call.loc(), resultValue, returnType);
            } else {
                throw new IllegalStateException("Calling int literal method on non-int-literal? Bug in compiler, please report");
            }
        }, null);
    }

    private <T> List<MethodDef> generateUnary(String name, Function<BigInteger, T> func, TypeDef mappedIntLiteralType, List<TypeDef> mappedIntTypes) {
        ArrayList<MethodDef> list = new ArrayList<>();
        //Literal -> Literal
        list.add(generateUnary(name, func, mappedIntLiteralType, mappedIntLiteralType));
        for (TypeDef mappedIntType : mappedIntTypes)
            //Literal -> Concrete
            list.add(generateUnary(name, func, mappedIntLiteralType, mappedIntType));
        list.trimToSize();
        return list;
    }

    @Override
    public List<MethodDef> getMethods(TypeChecker checker, List<TypeDef> generics) {
        TypeDef mappedIntLiteralType = checker.getBasicBuiltin(INSTANCE);
        TypeDef mappedBoolType = checker.getBasicBuiltin(BoolType.INSTANCE);
        List<TypeDef> mappedIntTypes = ListUtils.map(IntegerType.ALL_INT_TYPES, checker::getBasicBuiltin);
        return ListUtils.join(List.of(
                generateBinary("add", BigInteger::add, mappedIntLiteralType, mappedIntTypes, checker),
                generateBinary("sub", BigInteger::subtract, mappedIntLiteralType, mappedIntTypes, checker),
                generateBinary("mul", BigInteger::multiply, mappedIntLiteralType, mappedIntTypes, checker),
                generateBinary("div", BigInteger::divide, mappedIntLiteralType, mappedIntTypes, checker),
                generateBinary("rem", BigInteger::remainder, mappedIntLiteralType, mappedIntTypes, checker),

                generateBinary("band", BigInteger::and, mappedIntLiteralType, mappedIntTypes, checker),
                generateBinary("bor", BigInteger::or, mappedIntLiteralType, mappedIntTypes, checker),
                generateBinary("bxor", BigInteger::xor, mappedIntLiteralType, mappedIntTypes, checker),

                generateComparison("gt", (a, b) -> a.compareTo(b) > 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, checker),
                generateComparison("lt", (a, b) -> a.compareTo(b) < 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, checker),
                generateComparison("ge", (a, b) -> a.compareTo(b) >= 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, checker),
                generateComparison("le", (a, b) -> a.compareTo(b) <= 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, checker),
                generateComparison("eq", (a, b) -> a.compareTo(b) == 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, checker),

                generateUnary("neg", BigInteger::negate, mappedIntLiteralType, mappedIntTypes),
                generateUnary("bnot", BigInteger::not, mappedIntLiteralType, mappedIntTypes)
//                List.of(generateUnary("not", b -> b.equals(BigInteger.ZERO), mappedIntLiteralType, mappedBoolType))
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
    public TypeDef compileTimeToRuntimeConvert(TypeDef thisType, Loc loc, TypeChecker checker) throws CompilationException {
        throw new TypeCheckingException("Unable to determine type of int literal. Try adding annotations!", loc);
    }
}
