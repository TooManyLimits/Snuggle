package ast.type_resolved;

import ast.passes.TypeChecker;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedConstructor;
import ast.typed.expr.TypedExpr;
import builtin_types.types.OptionType;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;

public class TypeCheckingHelper {

    /**
     * Wrap the given TypedExpr into an Option of its type.
     */
    public static TypedConstructor wrapInOption(Loc loc, TypedExpr toBeWrapped, TypeChecker checker, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypeDef optionWrapped = checker.getGenericBuiltin(OptionType.INSTANCE, List.of(toBeWrapped.type()), loc, cause);
        //And return an Option constructor around this.
        MethodDef constructor = ListUtils.find(optionWrapped.methods(), method ->
                method.name().equals("new") && method.paramTypes().size() == 1); //Get the constructor
        return new TypedConstructor(loc, optionWrapped, constructor, List.of(toBeWrapped));
    }

    public static TypedConstructor getEmptyOption(Loc loc, TypeDef optionGeneric, TypeChecker checker, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        TypeDef optionWrapped = checker.getGenericBuiltin(OptionType.INSTANCE, List.of(optionGeneric), loc, cause);
        //And return an Option constructor around this.
        MethodDef constructor = ListUtils.find(optionWrapped.methods(), method ->
                method.name().equals("new") && method.paramTypes().size() == 0); //Get the constructor
        return new TypedConstructor(loc, optionWrapped, constructor, List.of());
    }

}
