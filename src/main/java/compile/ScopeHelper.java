package compile;

import ast.typed.Type;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import exceptions.compile_time.AlreadyDeclaredException;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.UndeclaredVariableException;
import lexing.Loc;
import util.MapStack;

import java.util.Stack;

/**
 * Manages local variable indices and such. An instance is created
 * when compiling a method body.
 */
public class ScopeHelper {

    private final Stack<Integer> indicesStack = new Stack<>();
    private int curIndex = 0;
    private final MapStack<String, Integer> localVariables = new MapStack<>();

    public int declare(Loc loc, Compiler compiler, String name, Type type) throws CompilationException {
        TypeDef def = compiler.getTypeDef(type);
        int slots = 1;
        if (def instanceof BuiltinTypeDef b && (
                (b.builtin() instanceof IntegerType i && i.bits == 64) ||
                (b.builtin() instanceof FloatType f && f.bits == 64)
        )) slots = 2;
        return declare(loc, name, slots);
    }

    // long and double use 2 slots, everything else uses 1
    // (yes, references *do* only use 1, despite being 64 bits,
    // I think it's for backwards compatibility or something)
    public int declare(Loc loc, String name, int slots) throws CompilationException {
        if (slots < 1 || slots > 2) throw new IllegalArgumentException("Illegal slot count? Bug in compiler, please report");
        if (localVariables.putIfAbsent(name, curIndex) != null)
            throw new AlreadyDeclaredException("Variable \"" + name + "\" is already declared in this scope", loc);
        curIndex += slots;
        return curIndex - slots;
    }

    public int lookup(Loc loc, String name) throws CompilationException {
        Integer index = localVariables.get(name);
        if (index == null)
            throw new UndeclaredVariableException("Variable \"" + name + "\" is not declared in this scope", loc);
        return index;
    }

    public void push() {
        indicesStack.push(curIndex);
        localVariables.push();
    }

    public void pop() {
        curIndex = indicesStack.pop();
        localVariables.pop();
    }



}
