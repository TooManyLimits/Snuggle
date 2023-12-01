package ast.typed.def.type;

import ast.typed.def.field.BuiltinFieldDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.MethodDef;
import exceptions.compile_time.CompilationException;
import util.GenericStringUtil;
import util.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TupleTypeDef implements TypeDef {

    public final List<TypeDef> elementTypes;
    private final String name, runtimeName;
    private final List<String> descriptor;
    private final int stackSlots;
    private final List<FieldDef> fields;
    private final ArrayList<MethodDef> methods;

    public TupleTypeDef(List<TypeDef> elements) {
        elementTypes = elements;
        name = elementTypes.size() == 0 ? "()" : GenericStringUtil.instantiateName("", elementTypes);
        runtimeName = "tuples/" + name;
        stackSlots = elements.stream().map(TypeDef::stackSlots).reduce(Integer::sum).orElse(0);
        fields = ListUtils.mapIndexed(elements, (e, i) ->
                new BuiltinFieldDef("v" + i, this, e, false));
        methods = new ArrayList<>(0);
        descriptor = ListUtils.join(ListUtils.map(nonStaticFields(), f -> f.type().getDescriptor()));
    }

    @Override
    public List<TypeDef> generics() {
        return elementTypes;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String runtimeName() {
        return runtimeName;
    }

    @Override
    public boolean isReferenceType() {
        return false;
    }

    @Override
    public boolean isPlural() {
        return true;
    }

    @Override
    public boolean extensible() {
        return false;
    }

    @Override
    public int stackSlots() {
        return stackSlots;
    }

    @Override
    public Set<TypeDef> typeCheckingSupertypes() throws CompilationException {
        return Set.of();
    }

    @Override
    public TypeDef inheritanceSupertype() throws CompilationException {
        return null;
    }

    @Override
    public List<FieldDef> fields() {
        return fields;
    }

    @Override
    public List<MethodDef> methods() {
        return methods;
    }

    @Override
    public void addMethod(MethodDef newMethod) {
        methods.add(newMethod);
    }

    @Override
    public List<String> getDescriptor() {
        return descriptor;
    }

    @Override
    public String getReturnTypeDescriptor() {
        return "V";
    }

    @Override
    public void checkCode() throws CompilationException {
        //do nothing, no code to check
    }

    @Override
    public boolean hasSpecialConstructor() {
        return true;
    }


}
