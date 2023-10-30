package ast.typed.expr;

import ast.typed.def.field.FieldDef;

import java.util.ArrayList;
import java.util.List;

//Linked list of desired fields
public record DesiredFieldNode(FieldDef field, DesiredFieldNode next) {

    public static List<FieldDef> toList(DesiredFieldNode node) {
        //Easy cases
        if (node == null)
            return List.of();
        if (node.next == null)
            return List.of(node.field);

        ArrayList<FieldDef> res = new ArrayList<>();
        while (node != null) {
            res.add(node.field);
            node = node.next;
        }
        res.trimToSize();
        return res;
    }

}
