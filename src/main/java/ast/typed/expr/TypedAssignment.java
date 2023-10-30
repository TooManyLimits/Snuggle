package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.objects.GetField;
import ast.ir.instruction.objects.SetField;
import ast.ir.instruction.stack.Dup;
import ast.ir.instruction.vars.LoadLocal;
import ast.ir.instruction.vars.StoreLocal;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;

public record TypedAssignment(Loc loc, TypedExpr lhs, TypedExpr rhs, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock code, DesiredFieldNode desiredFields) throws CompilationException {

        //Follow the FieldAccesses leftwards, until we either hit a reference type, or we hit a local variable.
        TypedExpr lhs = lhs();
        List<FieldDef> fieldsToFollow = new ArrayList<>();
        int indexOffset = 0;
        while (lhs instanceof TypedFieldAccess fieldAccess) {
            fieldsToFollow.add(0, fieldAccess.field()); //Prepend the field to our list to follow
            for (FieldDef field : fieldAccess.lhs().type().fields()) {
                if (field == fieldAccess.field())
                    break;
                indexOffset += fieldAccess.field().type().stackSlots();
            }
            lhs = fieldAccess.lhs();
            if (lhs.type().isReferenceType())
                break;

        }

        //Now, assert that the lhs is either:
        // - A local variable
        // - Outputs a reference type
        if (!(lhs instanceof TypedVariable || lhs.type().isReferenceType()))
            throw new TypeCheckingException("Cannot set field \"" + fieldsToFollow.get(0).name() + "\" on this struct; as this struct is neither a local variable nor a field of a reference type!", lhs.loc());

        //If fieldsToFollow.size() > 0, then this is a field set; otherwise, it's a local variable.
        if (lhs instanceof TypedVariable typedVariable && (fieldsToFollow.size() == 0 || typedVariable.type().isPlural())) {
            //Local variable. Get the mapped index and compile the rhs.
            int mappedIndex = code.env.lookup(loc, typedVariable.name()) + indexOffset;
            rhs.compile(code, null);
            code.emit(new StoreLocal(mappedIndex, type)); //Store the local

            DefIndexPair desiredIndex = getDesiredIndexOffset(type, mappedIndex, desiredFields); //Get the desired index/type
            code.emit(new LoadLocal(desiredIndex.index, desiredIndex.def.get())); //Load-local with that

        } else if (fieldsToFollow.size() > 0 && lhs.type().isReferenceType()) {
            //Setting field of a reference type
            lhs.compile(code, null); //Compile lhs
            code.emit(new Dup(lhs.type())); //Dup it
            rhs.compile(code, null); //Compile rhs
            code.emit(new SetField(fieldsToFollow)); //Set field
            code.emit(new GetField(ListUtils.join(fieldsToFollow, DesiredFieldNode.toList(desiredFields)))); //Fetch the desired value back
        } else {
            throw new IllegalStateException("Bug in compiler - illegal state in TypedAssignment. Please report!");
        }
    }
    public record DefIndexPair(TypeDef def, int index) {} //Return type from below
    public static DefIndexPair getDesiredIndexOffset(TypeDef curDef, int curIndex, DesiredFieldNode desiredFields) {
        if (desiredFields == null)
            return new DefIndexPair(curDef, curIndex);
        List<FieldDef> fieldDefs = curDef.fields();
        for (FieldDef fieldDef : fieldDefs) {
            if (fieldDef == desiredFields.field()) {
                return getDesiredIndexOffset(fieldDef.type(), curIndex, desiredFields.next());
            } else {
                if (!fieldDef.isStatic())
                    curIndex += fieldDef.type().stackSlots();
            }
        }
        throw new IllegalStateException("Should have found field by now - bug in compiler, please report!");
    }
}
