package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.vars.LoadLocal;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.Set;

public record TypedVariable(Loc loc, String name, TypeDef type) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {

    }

    @Override
    public void compile(CodeBlock code, DesiredFieldNode desiredFields) throws CompilationException {
        int startIndex = code.env.lookup(loc, name);
        TypedAssignment.DefIndexPair offsetIndex = TypedAssignment.getDesiredIndexOffset(type, startIndex, desiredFields);
        code.emit(new LoadLocal(offsetIndex.index(), offsetIndex.def().get()));
    }

}
