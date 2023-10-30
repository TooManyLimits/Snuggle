package ast.ir.instruction.objects;

import ast.ir.instruction.Instruction;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

//Get from a field. The receiver (a reference type) is on the stack.
//The list of fields to follow happens when doing something like a.x.y.z,
//where the fields "x" and "y" have plural types. In this case, the list has length 3, "x" "y" and "z".
//In the usual case, a.z, the length is only 1. "z".
public record GetField(List<FieldDef> fieldsToFollow) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) throws CompilationException {

        FieldDef firstField = fieldsToFollow.get(0);
        FieldDef lastField = fieldsToFollow.get(fieldsToFollow.size() - 1);

        String owner = firstField.owningType().runtimeName();
        boolean isStatic = firstField.isStatic();
        int opcode = isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD;

        String fieldName = firstField.name();
        if (fieldsToFollow.size() > 1) {
            StringBuilder nameBuilder = new StringBuilder();
            for (FieldDef def : fieldsToFollow)
                nameBuilder.append(def).append("$");
            nameBuilder.deleteCharAt(nameBuilder.length() - 1); //Delete trailing $
            fieldName = nameBuilder.toString();
        }

        get(opcode, owner, jvm, fieldName, lastField.type());
    }

    private void get(int opcode, String owner, MethodVisitor jvm, String fieldName, TypeDef type) {
        if (type.isPlural()) {
            List<FieldDef> innerFields = type.fields();
            for (FieldDef innerField : innerFields) {
                if (innerField.isStatic()) continue;
                String builtName = fieldName + "$" + innerField.name();
                get(opcode, owner, jvm, builtName, innerField.type());
            }
        } else {
            jvm.visitFieldInsn(opcode, owner, fieldName, type.getDescriptor().get(0));
        }
    }


    @Override
    public long cost() {
        return 1;
    }
}
