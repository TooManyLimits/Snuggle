package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.misc.LineNumber;
import ast.ir.instruction.stack.Push;
import ast.ir.instruction.vars.LoadThis;
import ast.ir.instruction.objects.MethodCall;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import lexing.Loc;
import runtime.Unit;

import java.util.List;

public record TypedSuperMethodCall(Loc loc, TypeDef receiverType, MethodDef method, List<TypedExpr> args, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock code) {
        code.emit(new LoadThis(receiverType));
        for (TypedExpr arg : args)
            arg.compile(code);
        code.emit(new LineNumber(loc.startLine()));
        code.emit(new MethodCall(true, method));
        if (method.isConstructor())
            code.emit(new Push(loc, Unit.INSTANCE, type));
    }
}
