package ast.ir.instruction.objects;

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
    public void accept(MethodVisitor jvm) throws CompilationException {

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

        set(opcode, owner, jvm, fieldName, lastField.type());
    }

    private void set(int opcode, String owner, MethodVisitor jvm, String fieldName, TypeDef type) {
        if (type.isPlural()) {
            List<FieldDef> innerFields = type.fields();
            ListUtils.iterBackwards(innerFields, innerField -> {
                if (innerField.isStatic()) return;
                String builtName = fieldName + "$" + innerField.name();
                set(opcode, owner, jvm, builtName, innerField.type());
            });
        } else {
            jvm.visitFieldInsn(opcode, owner, fieldName, type.getDescriptor().get(0));
        }
    }

    @Override
    public long cost() {
        return 1;
    }
}
