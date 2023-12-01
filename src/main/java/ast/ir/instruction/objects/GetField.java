package ast.ir.instruction.objects;

import ast.ir.def.CodeBlock;
import ast.ir.helper.BytecodeHelper;
import ast.ir.instruction.Instruction;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.GenericStringUtil;

import java.util.List;

//Get from a field. The receiver (a reference type) is on the stack.
//The list of fields to follow happens when doing something like a.x.y.z,
//where the fields "x" and "y" have plural topLevelTypes. In this case, the list has length 3, "x" "y" and "z".
//In the usual case, a.z, the length is only 1. "z".
public record GetField(List<FieldDef> fieldsToFollow) implements Instruction {

    @Override
    public void accept(CodeBlock block, MethodVisitor jvm) throws CompilationException {

        FieldDef firstField = fieldsToFollow.get(0);
        FieldDef lastField = fieldsToFollow.get(fieldsToFollow.size() - 1);

        String owner = firstField.owningType().runtimeName();
        boolean isStatic = firstField.isStatic();
        int opcode = isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD;

        String fieldName = firstField.name();
        if (fieldsToFollow.size() > 1) {
            StringBuilder nameBuilder = new StringBuilder();
            for (FieldDef def : fieldsToFollow)
                nameBuilder.append(def.name()).append("$");
            nameBuilder.deleteCharAt(nameBuilder.length() - 1); //Delete trailing $
            fieldName = nameBuilder.toString();
        }

        get(opcode, firstField.owningType(), owner, jvm, fieldName, lastField.type(), lastField.type().isPlural(), block.env.maxIndex());
    }

    private void get(int opcode, TypeDef ownerType, String owner, MethodVisitor jvm, String fieldName, TypeDef type, boolean isPlural, int maxIndex) {
        if (type.isPlural()) {
            List<FieldDef> innerFields = type.nonStaticFields();
            for (FieldDef innerField : innerFields) {
                String builtName = fieldName + "$" + innerField.name();
                get(opcode, ownerType, owner, jvm, builtName, innerField.type(), isPlural, maxIndex);
            }
        } else {
            if (isPlural && opcode != Opcodes.GETSTATIC) {
                BytecodeHelper.visitVariable(maxIndex, ownerType, false, jvm); //load variable
//                BytecodeHelper.swap(jvm, type, ownerType); //swap
            }
            jvm.visitFieldInsn(opcode, owner, GenericStringUtil.mangleSlashes(fieldName), type.getDescriptor().get(0)); //get field
        }
    }


    @Override
    public long cost() {
        return 1;
    }
}
