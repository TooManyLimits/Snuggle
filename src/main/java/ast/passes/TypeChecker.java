package ast.passes;

import ast.typed.def.type.TypeDef;
import exceptions.AlreadyDeclaredException;
import exceptions.CompilationException;
import ast.type_resolved.prog.TypeResolvedAST;
import ast.typed.Type;
import ast.typed.prog.TypedAST;
import ast.typed.prog.TypedFile;
import exceptions.UndeclaredVariableException;
import lexing.Loc;
import util.MapStack;
import util.MapUtils;

import java.util.List;
import java.util.Map;

/**
 * Responsible for converting a TypeResolvedAST into
 * the next stage: a TypedAST.
 *
 * Figures out the types of all expressions and such.
 */
public class TypeChecker {

    // The annotatedType pool, keeps track of all the types in existence
    private final TypePool typePool;

    private TypeChecker(TypeResolvedAST ast) throws CompilationException {
        typePool = new TypePool(this, ast);
    }

    public TypePool pool() {
        return typePool;
    }

    private final MapStack<String, Type> scopeVariables = new MapStack<>();
    public void push() { scopeVariables.push(); }
    public void pop() { scopeVariables.pop(); }
    public void declare(Loc loc, String name, Type type) throws CompilationException {
        Type prevType = scopeVariables.putIfAbsent(name, type);
        if (prevType != null)
            throw new AlreadyDeclaredException("Variable \"" + name + "\" is already declared in this scope!", loc);
    }

    public Type lookup(Loc loc, String name) throws CompilationException {
        Type t = scopeVariables.get(name);
        if (t == null)
            throw new UndeclaredVariableException("Variable \"" + name + "\" was not declared in this scope", loc);
        return t;
    }

    /**
     * Method to fully convert a TypeResolvedAST into a TypedAST.
     */
    public static TypedAST type(TypeResolvedAST resolvedAST) throws CompilationException {
        //Create the checker
        TypeChecker checker = new TypeChecker(resolvedAST);
        //Type check all the top-level code
        Map<String, TypedFile> typedFiles = MapUtils.mapValues(resolvedAST.files(), file -> file.type(checker));
        //Type check the method bodies, repeatedly, until there are no more
        //NOTE: This cannot be an enhanced for loop! Because:
        //while we check method bodies, *more checked type defs can be added to the list*.
        //If we used an enhanced loop, this would lead to concurrent modification exceptions.
        //However, this way, since new types are always appended to the end, we continue
        //checking method bodies until no new method bodies are added to check, and we reach
        //the end of the list.
        List<TypeDef> checkedTypeDefs = checker.pool().getFinalTypeDefs();
        for (int i = 0; i < checkedTypeDefs.size(); i++)
            checkedTypeDefs.get(i).checkMethodBodies();
        //Return the result
        return new TypedAST(checkedTypeDefs, typedFiles);
    }
}
