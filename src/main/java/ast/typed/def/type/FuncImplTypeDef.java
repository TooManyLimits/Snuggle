package ast.typed.def.type;

import ast.passes.TypeChecker;
import ast.typed.def.field.FieldDef;
import ast.typed.def.method.*;
import exceptions.compile_time.CompilationException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Set;

public class FuncImplTypeDef implements TypeDef {

    private final FuncTypeDef funcToImplement;
    private final String name, runtimeName, descriptor;
    private final List<MethodDef> methods;

    public FuncImplTypeDef(TypeChecker checker, FuncTypeDef funcToImplement, SnuggleMethodDef generatedMethod) {
//                           Loc loc, LateInitFunction<List<TypeDef>, TypedExpr, CompilationException> body) {
        this.funcToImplement = funcToImplement;
        name = "Impl_" + funcToImplement.implIndex.getAndIncrement();
        runtimeName = "lambdas/" + funcToImplement.name() + "/" + name;
        descriptor = "L" + runtimeName + ";";
        TypeDef unitType = checker.getTuple(List.of());
        methods = List.of(
                generatedMethod,
                new CustomMethodDef("new", false, this, List.of(), unitType, (isSuperCall, block, desiredFields, jvm) -> {
                    jvm.visitMethodInsn(Opcodes.INVOKESPECIAL, runtimeName, "<init>", "()V", false);
                }, classWriter -> {
                    MethodVisitor methodWriter = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                    methodWriter.visitCode();
                    methodWriter.visitVarInsn(Opcodes.ALOAD, 0);
                    methodWriter.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                    methodWriter.visitInsn(Opcodes.RETURN);
                    methodWriter.visitEnd();
                })
        );
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
        return List.of();
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
    public void checkCode() throws CompilationException {}
}
