package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.stack.Pop;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;
import java.util.Set;

public record TypedStructConstructor(Loc loc, TypeDef type, List<TypedExpr> values) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {
        for (TypedExpr e : values)
            e.findAllThisFieldAccesses(setToFill);
    }

    @Override
    public void compile(CodeBlock block, DesiredFieldNode desiredFields) throws CompilationException {

        int i = 0;
        for (FieldDef field : type.nonStaticFields()) {

            if (desiredFields != null && desiredFields.field() == field) {
                //If we desire this field specifically, then push it
                values.get(i++).compile(block, desiredFields.next());
            } else {
                if (values.get(i) instanceof TypedLiteral || values.get(i) instanceof TypedVariable) {
                    //If the value is a literal or variable (has no side effects)
                    if (desiredFields != null) {
                        //If we have a desired field, but this was not it, then we'd pop
                        //the value anyway. So let's just not push it in the first place.
                        i++;
                    } else {
                        //The value has no side effects, but we don't have any desired
                        //fields, so we need to push it.
                        values.get(i++).compile(block, null);
                    }
                } else {
                    //The value is not a literal. So let's compile it, as it may have side effects.
                    values.get(i).compile(block, null);
                    //If we desire a specific field, then let's pop this.
                    if (desiredFields != null) {
                        block.emit(new Pop(values.get(i).type()));
                    }
                    i++; //remember to increment
                }
            }
        }

    }
}
