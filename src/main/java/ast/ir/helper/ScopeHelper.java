package ast.ir.helper;

import ast.typed.def.type.TypeDef;
import builtin_types.types.primitive.FloatType;
import builtin_types.types.primitive.IntegerType;
import lexing.Loc;
import util.MapStack;

import java.util.Stack;

/**
 * Manages local variable indices and such. An instance is created
 * when compiling a method body.
 */
public class ScopeHelper {

    private final Stack<Integer> indicesStack = new Stack<>();
    private int curIndex = 0, maxIndex = 0;
    private final MapStack<String, Integer> localVariables = new MapStack<>();

    public int declare(Loc loc, String name, TypeDef type) {
        if (type.isPlural()) {
            return declare(loc, name, type.stackSlots());
        } else if ((type.builtin() instanceof IntegerType i && i.bits == 64) ||
                (type.builtin() instanceof FloatType f && f.bits == 64)) {
            return declare(loc, name, 2);
        } else {
            return declare(loc, name, 1);
        }
    }

    //Get the max index this has gone to
    public int maxIndex() {
        return maxIndex;
    }

    // long and double use 2 slots, everything else uses 1
    // (yes, references *do* only use 1, despite being 64 bits,
    // I think it's for backwards compatibility or something)
    public int declare(Loc loc, String name, int slots) {
        if (localVariables.putIfAbsent(name, curIndex) != null)
            throw new IllegalStateException("Variable \"" + name + "\" is already declared in this scope - but this should have been caught earlier? Bug in compiler, please report!");
        curIndex += slots;
        maxIndex = Math.max(maxIndex, curIndex);
        return curIndex - slots;
    }

    public int lookup(Loc loc, String name) {
        Integer index = localVariables.get(name);
        if (index == null)
            throw new IllegalStateException("Variable \"" + name + "\" is not declared in this scope - but this should have been caught earlier? Bug in compiler, please report!");
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
