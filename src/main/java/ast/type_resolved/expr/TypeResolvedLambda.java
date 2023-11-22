package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.def.field.BuiltinFieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import ast.typed.def.type.*;
import ast.typed.expr.TypedConstructor;
import ast.typed.expr.TypedExpr;
import builtin_types.types.ObjType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.LateInit;
import util.LateInitFunction;
import util.ListUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public record TypeResolvedLambda(Loc loc, List<String> paramNames, TypeResolvedExpr body) implements TypeResolvedExpr {
    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        body.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        throw new TypeCheckingException("Unable to infer type of lambda expression. Try adding type annotations or using it in a context that expects a certain lambda.", loc, cause);
    }

    public TypeDef inferTypeGivenArgTypes(List<TypeDef> argTypes, TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        //Create a temporary type def to hold the fields
        IndirectTypeDef indirect = new IndirectTypeDef();
        ClassDef tempDef = new ClassDef(loc, "##SnuggleClosureEnvironment##",
                new LateInit<>(() -> checker.getBasicBuiltin(ObjType.INSTANCE)),
                -1337,
                List.of(),
                ListUtils.map(List.copyOf(checker.peekEnv().getFlattenedMap().entrySet()), e ->
                        new BuiltinFieldDef(e.getKey(), indirect, e.getValue(), false)),
                List.of()
        );
        indirect.fill(tempDef);
        //Push a new environment with no particular desired output
        checker.pushNewEnv(true, new LateInit<>(() -> null));
        //Bind "this" in the scope
        checker.declare(loc, "this", indirect);
        //Bind the param names to the given arg types in the environment
        if (argTypes.size() != paramNames.size())
            throw new IllegalStateException("Attempting to infer lambda with wrong number of generic args? Bug in compiler, please report");
        for (int i = 0; i < argTypes.size(); i++)
            checker.declare(loc, paramNames.get(i), argTypes.get(i));
        //Infer the type of the body in the extended environment
        TypedExpr inferredBody;
        try {
            inferredBody = body.infer(currentType, checker, typeGenerics, methodGenerics, cause);
        } catch (CompilationException | RuntimeException e) {
            //Pop env even if it errors
            checker.popEnv();
            throw e;
        }
        //Figure out the return type
        Set<TypeDef> typeDefs = checker.getAttemptedReturnTypes();
        checker.popEnv();
        //Coalesce the return types into one common supertype, if possible
        typeDefs.add(inferredBody.type());
        TypeDef commonSupertype = TypeChecker.getCommonSupertype(typeDefs);
        if (commonSupertype == null)
            throw new TypeCheckingException("Could not find common supertype for returned types: " + typeDefs, loc, cause);
        //Return the func type def
        return new FuncTypeDef(checker, argTypes, commonSupertype);
    }

    @Override
    public TypedExpr check(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef expected, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        if (expected.get() instanceof FuncTypeDef funcTypeDef) {
            //Create a new SnuggleMethodDef, binding the expected variables, and check if it works out.
            IndirectTypeDef indirect = new IndirectTypeDef();
            SnuggleMethodDef generatedSnuggleMethod = new SnuggleMethodDef(loc, true, "invoke", 0, 0, false, false, indirect, paramNames.size(), paramNames,
                    new LateInitFunction<>(unused -> funcTypeDef.paramTypes),
                    new LateInitFunction<>(unused -> funcTypeDef.resultType),
                    new LateInitFunction<>(unused -> {
                        checker.pushNewEnv(true, new LateInit<>(() -> funcTypeDef.resultType));
                        //Declare "this"
                        checker.declare(loc, "this", indirect);
                        //Declare params
                        for (int i = 0; i < paramNames.size(); i++)
                            checker.declare(loc, paramNames.get(i), funcTypeDef.paramTypes.get(i));
                        try {
                            TypedExpr res = body.check(currentType, checker, typeGenerics, methodGenerics, funcTypeDef.resultType, cause);
                            checker.popEnv();
                            return res;
                        } catch (Throwable e) {
                            checker.popEnv();
                            throw e;
                        }
                    }));
            //Create the impl type def and fill in the indirect
            FuncImplTypeDef implDef = new FuncImplTypeDef(checker, funcTypeDef, generatedSnuggleMethod);
            indirect.fill(implDef);
            //Now check the snuggle method body, and also remove any redundant closure fields
            implDef.finalizeImpl(checker);
            //Register the type def
            checker.registerTypeDef(implDef);
            //Construct it
            MethodDef constructor = ListUtils.find(implDef.methods(), MethodDef::isConstructor);
            List<TypedExpr> args = ListUtils.map(implDef.fields(), f ->
                    new TypeResolvedVariable(loc, f.name(), false)
                            .check(currentType, checker, typeGenerics, methodGenerics, f.type(), cause)
            );
            return new TypedConstructor(loc, implDef, constructor, args);
        } else {
            throw new TypeCheckingException("Expected type \"" + expected.name() + "\", but lambda expression returns a function type", loc, cause);
        }
    }
}
