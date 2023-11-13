package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.objects.MethodCall;
import ast.ir.instruction.misc.LineNumber;
import ast.ir.instruction.objects.New;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;
import java.util.Set;

public record TypedConstructor(Loc loc, TypeDef type, MethodDef method, List<TypedExpr> args) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {
        for (TypedExpr e : args)
            e.findAllThisFieldAccesses(setToFill);
    }

    @Override
    public void compile(CodeBlock block, DesiredFieldNode desiredFields) throws CompilationException {
        if (!type.hasSpecialConstructor()) {
            //If this type doesn't have special constructor procedures:
            block.emit(new New(type.get())); //Emit a NEW, and dup the value. This is essentially compiling "the receiver".
        }

        for (TypedExpr arg : args)
            arg.compile(block, null);
        block.emit(new LineNumber(loc.startLine()));
        block.emit(new MethodCall(false, method, DesiredFieldNode.toList(desiredFields)));
    }
}
