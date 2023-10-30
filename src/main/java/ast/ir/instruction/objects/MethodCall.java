package ast.ir.instruction.objects;

import ast.ir.instruction.Instruction;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

//Call the given method. The receiver/args are on the stack already.
//The result of the call should be on the stack at the end.
//If desiredFields is non-empty, then this method returns a struct (or is a struct constructor), and we
//should only push the desired fields on the stack instead of the whole struct.
public record MethodCall(boolean isSuperCall, MethodDef methodToCall, List<FieldDef> desiredFields) implements Instruction {

    @Override
    public void accept(MethodVisitor jvm) {
        //Call the method:
        methodToCall.compileCall(isSuperCall, jvm);

        //If the method needs special handling afterwards (for plural types) then add more bytecodes
        if (methodToCall.returnType().isPlural()) {
            fetchPluralFields(jvm, methodToCall.returnType());
        }
    }

    private void fetchPluralFields(MethodVisitor jvm, TypeDef pluralType) {
        StringBuilder namePrefix = new StringBuilder();
        TypeDef curType = pluralType;
        for (FieldDef def : desiredFields)
            namePrefix.append(def.name()).append("$");
        if (desiredFields.size() > 0) {
            namePrefix.deleteCharAt(namePrefix.length() - 1);
            curType = desiredFields.get(desiredFields.size() - 1).type();
        }
        fetchPluralFieldsRecurse(jvm, pluralType, namePrefix.toString(), curType);
    }

    //We want to fetch the fields of this plural type and put them on the stack
    //However, make sure we only push the fields requested by desiredFields, if desiredFields
    //is restricting them.
    private void fetchPluralFieldsRecurse(MethodVisitor jvm, TypeDef pluralType, String namePrefix, TypeDef curType) {
        String runtimeName = pluralType.runtimeName();
        if (curType.isPlural()) {
            for (FieldDef field : curType.fields()) {
                if (field.isStatic()) continue;
                if (namePrefix.length() == 0)
                    fetchPluralFieldsRecurse(jvm, pluralType, namePrefix + field.name(), field.type());
                else
                    fetchPluralFieldsRecurse(jvm, pluralType, namePrefix + "$" + field.name(), field.type());
            }
        } else {
            jvm.visitFieldInsn(Opcodes.GETSTATIC, runtimeName, namePrefix, curType.getDescriptor().get(0));
        }
    }


    @Override
    public long cost() {
        //If special case, more instructions
        if (methodToCall.returnType().isPlural())
            return 2 + methodToCall.returnType().stackSlots() / 2;
        return 1; //Say it costs 1 to call by default
    }
}
