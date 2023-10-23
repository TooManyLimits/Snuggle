package builtin_types.types.numbers;

import ast.passes.TypeChecker;
import ast.type_resolved.expr.TypeResolvedLiteral;
import ast.type_resolved.expr.TypeResolvedMethodCall;
import ast.typed.expr.TypedMethodCall;
import builtin_types.types.BoolType;
import exceptions.CompilationException;
import ast.passes.TypePool;
import ast.typed.Type;
import ast.typed.def.method.ConstMethodDef;
import ast.typed.def.method.MethodDef;
import ast.typed.expr.TypedLiteral;
import builtin_types.BuiltinType;
import exceptions.TypeCheckingException;
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
    private <T> ConstMethodDef generateBinary(String name, BiFunction<BigInteger, BigInteger, T> func, Type intLiteralType, IntegerType unmappedArgType, Type argType, Type returnType, TypePool pool) {
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
                            pool.getTypeDef(pool.getBasicBuiltin(unmappedArgType)).getMethods(),
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
    private List<ConstMethodDef> generateBinary(String name, BiFunction<BigInteger, BigInteger, BigInteger> func, Type mappedIntLiteralType, List<Type> mappedIntTypes, TypePool pool) {
        ArrayList<ConstMethodDef> list = new ArrayList<>();
        //Literal, Literal -> Literal
        list.add(generateBinary(name, func, mappedIntLiteralType, null, mappedIntLiteralType, mappedIntLiteralType, pool));
        for (int i = 0; i < mappedIntTypes.size(); i++) {
            IntegerType unmappedIntType = IntegerType.ALL_INT_TYPES.get(i);
            Type mappedIntType = mappedIntTypes.get(i);
            //Literal, Literal -> Concrete
            list.add(generateBinary(name, func, mappedIntLiteralType, null, mappedIntLiteralType, mappedIntType, pool));
            //Literal, Concrete -> Concrete
            list.add(generateBinary(name, func, mappedIntLiteralType, unmappedIntType, mappedIntType, mappedIntType, pool));
        }
        list.trimToSize();
        return list;
    }

    //Generate many comparison methods
    private List<ConstMethodDef> generateComparison(String name, BiFunction<BigInteger, BigInteger, Boolean> func, Type mappedIntLiteralType, Type mappedBoolType, List<Type> mappedIntTypes, TypePool pool) {
        ArrayList<ConstMethodDef> list = new ArrayList<>();
        //Literal, Literal -> Boolean
        list.add(generateBinary(name, func, mappedIntLiteralType, null, mappedIntLiteralType, mappedBoolType, pool));
        for (int i = 0; i < mappedIntTypes.size(); i++) {
            IntegerType unmappedIntType = IntegerType.ALL_INT_TYPES.get(i);
            Type mappedIntType = mappedIntTypes.get(i);
            //Literal, Concrete -> Boolean
            list.add(generateBinary(name, func, mappedIntLiteralType, unmappedIntType, mappedIntType, mappedBoolType, pool));
        }
        list.trimToSize();
        return list;
    }

    /**
     * Generate a unary ConstMethodDef with the given name and func
     */
    private <T> ConstMethodDef generateUnary(String name, Function<BigInteger, T> func, Type intLiteralType, Type returnType) {
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

    private <T> List<ConstMethodDef> generateUnary(String name, Function<BigInteger, T> func, Type mappedIntLiteralType, List<Type> mappedIntTypes) {
        ArrayList<ConstMethodDef> list = new ArrayList<>();
        //Literal -> Literal
        list.add(generateUnary(name, func, mappedIntLiteralType, mappedIntLiteralType));
        for (Type mappedIntType : mappedIntTypes)
            //Literal -> Concrete
            list.add(generateUnary(name, func, mappedIntLiteralType, mappedIntType));
        list.trimToSize();
        return list;
    }

    @Override
    public List<? extends MethodDef> getMethods(List<Type> generics, TypePool pool) throws CompilationException {
        Type mappedIntLiteralType = pool.getBasicBuiltin(INSTANCE);
        Type mappedBoolType = pool.getBasicBuiltin(BoolType.INSTANCE);
        List<Type> mappedIntTypes = ListUtils.map(IntegerType.ALL_INT_TYPES, pool::getBasicBuiltin);
        return ListUtils.join(List.of(
                generateBinary("add", BigInteger::add, mappedIntLiteralType, mappedIntTypes, pool),
                generateBinary("sub", BigInteger::subtract, mappedIntLiteralType, mappedIntTypes, pool),
                generateBinary("mul", BigInteger::multiply, mappedIntLiteralType, mappedIntTypes, pool),
                generateBinary("div", BigInteger::divide, mappedIntLiteralType, mappedIntTypes, pool),
                generateBinary("rem", BigInteger::remainder, mappedIntLiteralType, mappedIntTypes, pool),

                generateBinary("band", BigInteger::and, mappedIntLiteralType, mappedIntTypes, pool),
                generateBinary("bor", BigInteger::or, mappedIntLiteralType, mappedIntTypes, pool),
                generateBinary("bxor", BigInteger::xor, mappedIntLiteralType, mappedIntTypes, pool),

                generateComparison("gt", (a, b) -> a.compareTo(b) > 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, pool),
                generateComparison("lt", (a, b) -> a.compareTo(b) < 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, pool),
                generateComparison("ge", (a, b) -> a.compareTo(b) >= 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, pool),
                generateComparison("le", (a, b) -> a.compareTo(b) <= 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, pool),
                generateComparison("eq", (a, b) -> a.compareTo(b) == 0, mappedIntLiteralType, mappedBoolType, mappedIntTypes, pool),

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
    public String getDescriptor(List<Type> generics, TypePool pool) {
        return null;
    }

    @Override
    public Type toStorable(Type thisType, Loc loc, TypePool pool) throws CompilationException {
        throw new TypeCheckingException("Unable to determine type of int literal. Try adding annotations!", loc);
    }

    //Despite the integer types being supertypes for type-checking reasons,
    //this has no true supertype, in the sense of being able to perform virtual calls.
    @Override
    public Set<Type> getSupertypes(List<Type> generics, TypePool pool) throws CompilationException {
        //The supertypes of this are all the integer types
        return Set.copyOf(ListUtils.map(IntegerType.ALL_INT_TYPES, pool::getBasicBuiltin));
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
