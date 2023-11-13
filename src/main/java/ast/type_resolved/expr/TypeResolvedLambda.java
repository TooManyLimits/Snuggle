package ast.type_resolved.expr;

import ast.passes.GenericVerifier;
import ast.passes.TypeChecker;
import ast.typed.def.method.MethodDef;
import ast.typed.def.method.SnuggleMethodDef;
import ast.typed.def.type.FuncImplTypeDef;
import ast.typed.def.type.FuncTypeDef;
import ast.typed.def.type.IndirectTypeDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedConstructor;
import ast.typed.expr.TypedExpr;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.LateInitFunction;
import util.ListUtils;

import java.util.List;

public record TypeResolvedLambda(Loc loc, List<String> paramNames, TypeResolvedExpr body) implements TypeResolvedExpr {
    @Override
    public void verifyGenericArgCounts(GenericVerifier verifier) throws CompilationException {
        body.verifyGenericArgCounts(verifier);
    }

    @Override
    public TypedExpr infer(TypeDef currentType, TypeChecker checker, List<TypeDef> typeGenerics, List<TypeDef> methodGenerics, TypeDef.InstantiationStackFrame cause) throws CompilationException {
        throw new TypeCheckingException("Unable to infer type of lambda expression. Try adding type annotations or using it in a context that expects a certain lambda.", loc, cause);
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
                        checker.pushNewEnv(true);
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
