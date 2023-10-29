package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.objects.GetReferenceTypeField;
import ast.ir.instruction.stack.Dup;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import lexing.Loc;

public record TypedFieldAccess(Loc loc, TypedExpr lhs, FieldDef field, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock block) {
        if (!lhs.type().isPlural() && !type.isPlural()) {
            lhs.compile(block);
            block.emit(new GetReferenceTypeField(field));
        } else {
            throw new IllegalStateException("Plural type field accesses not yet supported");
        }
    }

    public void compileForSet(CodeBlock block) {
        if (lhs.type().isPlural())
            throw new IllegalStateException("Plural type field accesses not yet supported");
        lhs.compile(block);
        block.emit(new Dup(lhs.type()));
    }
}
