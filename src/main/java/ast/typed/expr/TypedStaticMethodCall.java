package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.misc.LineNumber;
import ast.ir.instruction.objects.MethodCall;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;
import java.util.Set;

public record TypedStaticMethodCall(Loc loc, TypeDef receiverType, MethodDef method, List<TypedExpr> args, TypeDef type) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {
        for (TypedExpr e : args)
            e.findAllThisFieldAccesses(setToFill);
    }


    @Override
    public void compile(CodeBlock code, DesiredFieldNode desiredFields) throws CompilationException {
        for (TypedExpr arg : args)
            arg.compile(code, null);
        code.emit(new LineNumber(loc.startLine()));
        code.emit(new MethodCall(false, method, DesiredFieldNode.toList(desiredFields)));
    }


}
