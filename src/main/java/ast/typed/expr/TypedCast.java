package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.objects.Cast;
import ast.ir.instruction.misc.LineNumber;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.util.Set;

public record TypedCast(Loc loc, int tokenLine, TypedExpr lhs, boolean isMaybe, TypeDef type) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {
        lhs.findAllThisFieldAccesses(setToFill);
    }

    @Override
    public void compile(CodeBlock block, DesiredFieldNode desiredFieldNode) throws CompilationException {
        if (desiredFieldNode != null)
            throw new IllegalStateException("DesiredFieldNode should always be null when compiling a cast? Bug in compiler, please report!");
        //Load lhs on the stack
        lhs.compile(block, null);
        //Emit line number
        block.emit(new LineNumber(loc.startLine()));
        //Emit cast
        if (isMaybe) {
            TypeDef innerType = ((BuiltinTypeDef) type).generics.get(0);
            block.emit(new Cast(lhs.type().get(), innerType.get(), true));
        } else {
            block.emit(new Cast(lhs.type().get(), type.get(), false));
        }

    }

}
