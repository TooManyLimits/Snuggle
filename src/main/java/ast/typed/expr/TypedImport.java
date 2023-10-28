package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.misc.LineNumber;
import ast.ir.instruction.misc.RunImport;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record TypedImport(Loc loc, String fileName, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock code) {

        code.emit(new LineNumber(loc.startLine()));
        code.emit(new RunImport(fileName));


    }
}
