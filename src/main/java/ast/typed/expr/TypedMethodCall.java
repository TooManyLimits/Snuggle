package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.misc.LineNumber;
import ast.ir.instruction.misc.RunBytecode;
import ast.ir.instruction.objects.MethodCall;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import util.ListUtils;

import java.util.List;
import java.util.Set;

public record TypedMethodCall(Loc loc, TypedExpr receiver, MethodDef method, List<TypedExpr> args, TypeDef type) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {
        receiver.findAllThisFieldAccesses(setToFill);
        for (TypedExpr e : args)
            e.findAllThisFieldAccesses(setToFill);
    }

    @Override
    public void compile(CodeBlock code, DesiredFieldNode desiredFields) throws CompilationException {
        receiver.compile(code, null);
        for (int i = 0; i < args.size(); i++) {
            //Compile the arg
            args.get(i).compile(code, null);
            //Apply the argument transformer if it exists
            RunBytecode b = method.getArgumentTransformer(i);
            if (b != null) code.emit(b);
        }
        code.emit(new LineNumber(loc.startLine()));
        code.emit(new MethodCall(false, method, DesiredFieldNode.toList(desiredFields)));
    }
}
