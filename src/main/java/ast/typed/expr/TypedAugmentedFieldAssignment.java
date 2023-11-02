package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.objects.GetField;
import ast.ir.instruction.objects.MethodCall;
import ast.ir.instruction.objects.SetField;
import ast.ir.instruction.stack.Dup;
import ast.ir.instruction.vars.LoadLocal;
import ast.ir.instruction.vars.StoreLocal;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;

//a.x += b
//a.x is lhs, b is rhs
//a.x = a.x.method(b)
public record TypedAugmentedFieldAssignment(Loc loc, MethodDef method, TypedExpr lhs, TypedExpr rhs, TypeDef type) implements TypedExpr {

    //Very similar to TypedAssignment's code, slightly different things emitted to the CodeBlock arg
    @Override
    public void compile(CodeBlock code, DesiredFieldNode desiredFields) throws CompilationException {

        //Follow the FieldAccesses leftwards, until we hit
        //- a reference type
        //- a local variable
        //- a static field
        TypedExpr lhs = lhs();
        List<FieldDef> fieldsToFollow = new ArrayList<>();
        int indexOffset = 0;
        while (lhs instanceof TypedFieldAccess fieldAccess) {
            fieldsToFollow.add(0, fieldAccess.field()); //Prepend the field to our list to follow
            //Static field? we're done, quit the loop
            if (fieldAccess.field().isStatic()) {
                lhs = null;
                break;
            }
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
        // - null
        // - Outputs a reference type
        if (!(lhs instanceof TypedVariable || lhs == null || lhs.type().isReferenceType()))
            throw new TypeCheckingException("Cannot set field \"" + fieldsToFollow.get(0).name() + "\" on this struct; as this struct is neither a local variable nor a field of a reference type!", lhs.loc());

        //If fieldsToFollow.size() > 0, then this is a field set; otherwise, it's a local variable.
        if (lhs instanceof TypedVariable typedVariable && (fieldsToFollow.size() == 0 || typedVariable.type().isPlural())) {
            //Local variable. Get the mapped index and compile the rhs.
            int mappedIndex = code.env.lookup(loc, typedVariable.name()) + indexOffset;
            code.emit(new LoadLocal(mappedIndex, type)); //Load the local
            rhs.compile(code, null); //Compile the rhs
            code.emit(new MethodCall(false, method, List.of())); //Call the method
            code.emit(new StoreLocal(mappedIndex, type)); //Store the local

            TypedAssignment.DefIndexPair desiredIndex = TypedAssignment.getDesiredIndexOffset(type, mappedIndex, desiredFields); //Get the desired index/type
            code.emit(new LoadLocal(desiredIndex.index(), desiredIndex.def().get())); //Load-local with that

        } else if (fieldsToFollow.size() > 0 && lhs != null && lhs.type().isReferenceType()) {
            //Setting field of a reference type
            lhs.compile(code, null); //Compile lhs
            code.emit(new Dup(lhs.type())); //Dup it twice
            code.emit(new Dup(lhs.type()));
            code.emit(new GetField(fieldsToFollow)); //Get field x
            rhs.compile(code, null); //Compile rhs
            code.emit(new MethodCall(false, method, List.of())); //Call the method
            code.emit(new SetField(fieldsToFollow)); //Set field
            code.emit(new GetField(ListUtils.join(fieldsToFollow, DesiredFieldNode.toList(desiredFields)))); //Fetch the desired value back
        } else if (lhs == null) {
            //Setting static field
            code.emit(new GetField(fieldsToFollow)); //Get field
            rhs.compile(code, null); //Compile rhs
            code.emit(new MethodCall(false, method, List.of())); //Call the method
            code.emit(new SetField(fieldsToFollow)); //Set field
            code.emit(new GetField(ListUtils.join(fieldsToFollow, DesiredFieldNode.toList(desiredFields)))); //Fetch the desired value back
        } else {
            throw new IllegalStateException("Bug in compiler - illegal state in TypedAssignment. Please report!");
        }

    }
}
