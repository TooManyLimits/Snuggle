package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.misc.LineNumber;
import ast.ir.instruction.objects.MethodCall;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import lexing.Loc;

import java.util.List;

public record TypedMethodCall(Loc loc, TypedExpr receiver, MethodDef method, List<TypedExpr> args, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock code) {
        receiver.compile(code);
        for (TypedExpr arg : args)
            arg.compile(code);
        code.emit(new LineNumber(loc.startLine()));
        code.emit(new MethodCall(false, method));
    }
}
