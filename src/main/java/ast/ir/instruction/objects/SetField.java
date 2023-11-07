package ast.ir.instruction.objects;

import ast.ir.def.CodeBlock;
import ast.ir.helper.BytecodeHelper;
import ast.ir.instruction.Instruction;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.ListUtils;

import java.util.List;

//Set to a field. The receiver (a reference type), then value are on the stack.
//The list of fields to follow happens when doing something like a.x.y.z = 5,
//where the fields "x" and "y" have plural types.
//In this case, fieldsToFollow has length 3, containing fields "x" "y" "z".
//for the regular singlet case of a.z = 5, fieldsToFollow has length 1, and it's the "z" field.
public record SetField(List<FieldDef> fieldsToFollow) implements Instruction {

    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) throws CompilationException {

        FieldDef firstField = fieldsToFollow.get(0);
        FieldDef lastField = fieldsToFollow.get(fieldsToFollow.size() - 1);

        String owner = firstField.owningType().runtimeName();
        boolean isStatic = firstField.isStatic();
        int opcode = isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;

        String fieldName = firstField.name();
        if (fieldsToFollow.size() > 1) {
            StringBuilder nameBuilder = new StringBuilder();
            for (FieldDef def : fieldsToFollow)
                nameBuilder.append(def.name()).append("$");
            nameBuilder.deleteCharAt(nameBuilder.length() - 1); //Delete trailing $
            fieldName = nameBuilder.toString();
        }

        set(opcode, firstField.owningType(), owner, jvm, fieldName, lastField.type(), lastField.type().isPlural(), block.env.maxIndex());
    }

    //if isPlural, then before every set() we need to get the top local variable, then swap it accordingly.
    private void set(int opcode, TypeDef ownerType, String owner, MethodVisitor jvm, String fieldName, TypeDef type, boolean isPlural, int maxIndex) {
        if (type.isPlural()) {
            List<FieldDef> innerFields = type.fields();
            ListUtils.iterBackwards(innerFields, innerField -> {
                if (innerField.isStatic()) return;
                String builtName = fieldName + "$" + innerField.name();
                set(opcode, ownerType, owner, jvm, builtName, innerField.type(), isPlural, maxIndex);
            });
        } else {
            if (isPlural && opcode != Opcodes.PUTSTATIC) {
                BytecodeHelper.visitVariable(maxIndex, ownerType, false, jvm); //load variable
                BytecodeHelper.swap(jvm, type, ownerType); //swap
            }
            jvm.visitFieldInsn(opcode, owner, fieldName, type.getDescriptor().get(0)); //store field
        }
    }

    @Override
    public long cost() {
        return 1;
    }
}
