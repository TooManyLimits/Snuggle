package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.objects.Cast;
import ast.ir.instruction.misc.LineNumber;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import lexing.Loc;

public record TypedCast(Loc loc, int tokenLine, TypedExpr lhs, boolean isMaybe, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock block) {
        //Load lhs on the stack
        lhs.compile(block);
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
