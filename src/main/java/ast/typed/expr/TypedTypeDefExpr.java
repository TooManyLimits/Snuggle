package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public record TypedTypeDefExpr(TypeDef.InstantiationStackFrame cause, Loc loc, Supplier<Collection<TypeDef>> instantiatedDefGetter, TypeDef type) implements TypedExpr {
    @Override
    public void compile(CodeBlock block, DesiredFieldNode desiredFields) throws CompilationException {
        ArrayList<TypedExpr> exprs = new ArrayList<>();
        //For everything instantiated with this type...
        for (TypeDef type : instantiatedDefGetter.get()) {
            //Find its #init method, if it has one...
            MethodDef initMethod = ListUtils.find(type.methods(), m -> m.name().equals("#init"));
            if (initMethod == null)
                continue;
            //And call said method.
            exprs.add(new TypedStaticMethodCall(loc, type, initMethod, List.of(), type));
        }
        if (exprs.size() != 0) {
            new TypedBlock(loc, exprs, type).compile(block, desiredFields);
        }
    }
}
