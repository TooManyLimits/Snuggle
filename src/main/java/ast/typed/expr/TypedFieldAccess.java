package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.objects.GetField;
import ast.ir.instruction.stack.Dup;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;

public record TypedFieldAccess(Loc loc, TypedExpr lhs, FieldDef field, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock block, DesiredFieldNode desiredFields) throws CompilationException {

        if (lhs.type().isReferenceType()) {
            //If LHS is a reference type, then don't push our field into the desired stack.
            //Compile the LHS without any desired fields.
            lhs.compile(block, null);
            //Then, get the list of fields (now including our own)
            List<FieldDef> desiredList = DesiredFieldNode.toList(new DesiredFieldNode(field, desiredFields));
            block.emit(new GetField(desiredList));
        } else if (lhs.type().isPlural()) {
            //If the LHS is a plural type, then we should compile it, appending our own field to the desired stack.
            desiredFields = new DesiredFieldNode(field, desiredFields);
            lhs.compile(block, desiredFields);
        } else {
            throw new IllegalStateException("Non-reference type has a field, but also isn't plural? Bug in compiler, please report!");
        }
    }
}
