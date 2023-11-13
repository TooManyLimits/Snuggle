package ast.typed.def.type;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.objects.SetField;
import ast.ir.instruction.vars.LoadLocal;
import ast.passes.TypeChecker;
import ast.typed.def.field.BuiltinFieldDef;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.*;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedVariable;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.LateInit;
import util.ListUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FuncImplTypeDef implements TypeDef {

    private final FuncTypeDef funcToImplement;
    private final String name, runtimeName, descriptor;
    private final ArrayList<MethodDef> methods;
    private final ArrayList<FieldDef> fields;

    public FuncImplTypeDef(TypeChecker checker, FuncTypeDef funcToImplement, SnuggleMethodDef generatedMethod) {
        this.funcToImplement = funcToImplement;
        name = "Impl_" + funcToImplement.implIndex.getAndIncrement();
        runtimeName = "lambdas/" + funcToImplement.name() + "/" + name;
        descriptor = "L" + runtimeName + ";";
        //Create initial fields
        fields = new ArrayList<>();
        for (Map.Entry<String, TypeDef> entry : checker.peekEnv().getFlattenedMap().entrySet())
            fields.add(new BuiltinFieldDef(entry.getKey(), this, entry.getValue(), false));
        //Add generated method (we add the constructor later)
        methods = new ArrayList<>(2);
        methods.add(generatedMethod);
    }

    //Finalizes the impl!
    public void finalizeImpl(TypeChecker checker) throws CompilationException {
        //Check the method body
        checkCode();
        //Find out which fields were actually used
        TypedExpr body = ((SnuggleMethodDef) methods.get(0)).body().get(List.of());
        Set<String> necessaryClosureFields = new HashSet<>();
        //Remove all unused closure fields
        body.findAllThisFieldAccesses(necessaryClosureFields);
        fields.removeIf(x -> !necessaryClosureFields.contains(x.name()));
        //Generate the constructor
        TypeDef unitType = checker.getTuple(List.of());
        LateInit<String, RuntimeException> descriptor = new LateInit<>(() -> methods.get(1).getDescriptor());
        methods.add(new CustomMethodDef("new", false, this, ListUtils.map(fields, FieldDef::type), unitType, (isSuperCall, block, desiredFields, jvm) -> {
            //Just call the constructor lol
            jvm.visitMethodInsn(Opcodes.INVOKESPECIAL, runtimeName, "<init>", descriptor.get(), false);
        }, classWriter -> {
            MethodVisitor methodWriter = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", descriptor.get(), null, null);
            methodWriter.visitCode();
            //Call super() (object init)
            methodWriter.visitVarInsn(Opcodes.ALOAD, 0);
            methodWriter.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            //Fill all the fields
            AtomicInteger totalIndex = new AtomicInteger(1);
            ListUtils.forEachIndexed(fields, (field, unused) -> {
                new LoadLocal(0, this).accept(null, methodWriter); //load this
                new LoadLocal(totalIndex.get(), field.type()).accept(null, methodWriter); //load element
                totalIndex.addAndGet(field.type().stackSlots()); //increment by stackslots
                new SetField(List.of(field)).accept(new CodeBlock((MethodDef) null), methodWriter); //set field
            });
            methodWriter.visitInsn(Opcodes.RETURN);
            methodWriter.visitEnd();
        }));
    }

    @Override
    public boolean hasSpecialConstructor() {
        return false;
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
        return true;
    }

    @Override
    public boolean isPlural() {
        return false;
    }

    @Override
    public boolean extensible() {
        return false;
    }

    @Override
    public int stackSlots() {
        return 1;
    }

    @Override
    public Set<TypeDef> typeCheckingSupertypes() throws CompilationException {
        return Set.of(funcToImplement);
    }

    @Override
    public TypeDef inheritanceSupertype() throws CompilationException {
        return funcToImplement;
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
        throw new IllegalStateException("Should not ever add method on function impl? Bug in compiler, please report");
    }

    @Override
    public List<String> getDescriptor() {
        return List.of(descriptor);
    }

    @Override
    public String getReturnTypeDescriptor() {
        return descriptor;
    }

    @Override
    public void checkCode() throws CompilationException {
        methods.get(0).checkCode(); //Check the method...
    }

}
