package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.flow.Return;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.ParsingException;
import lexing.Loc;

public record TypedReturn(Loc loc, TypedExpr rhs, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock block, DesiredFieldNode desiredFields) throws CompilationException {
        if (block.methodDef == null)
            throw new ParsingException("Cannot return here - can only return inside a method", loc);
        if (!type().isSubtype(block.methodDef.returnType()))
            throw new ParsingException("Method \"" + block.methodDef.name() + "\" wants to return \"" + block.methodDef.returnType().name() + "\", but return-expression is returning \"" + type.name() + "\"", loc);
        rhs.compile(block, null);
        block.emit(new Return(block.methodDef, type));
    }
}
