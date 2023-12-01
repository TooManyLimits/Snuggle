package ast.typed.def.method;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.flow.Return;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import ast.typed.expr.TypedExpr;
import ast.typed.expr.TypedLiteral;
import ast.typed.expr.TypedMethodCall;
import ast.typed.expr.TypedStaticMethodCall;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import util.GenericStringUtil;
import util.LateInitFunction;
import util.ListUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SnuggleMethodDef(Loc loc, boolean pub, String name, int disambiguationIndex, int numGenerics, boolean isStatic, boolean inline, TypeDef owningType, int numParams, List<String> paramNames,
                               LateInitFunction<List<TypeDef>, List<TypeDef>, RuntimeException> paramTypeGetter,
                               LateInitFunction<List<TypeDef>, TypeDef, RuntimeException> returnTypeGetter,
                               LateInitFunction<List<TypeDef>, TypedExpr, CompilationException> body,
                               Map<List<TypeDef>, SnuggleMethodDef> instantiationCache) implements MethodDef {

    public SnuggleMethodDef(Loc loc, boolean pub, String name, int disambiguationIndex, int numGenerics, boolean isStatic, boolean inline, TypeDef owningType, int numParams, List<String> paramNames,
                            LateInitFunction<List<TypeDef>, List<TypeDef>, RuntimeException> paramTypeGetter,
                            LateInitFunction<List<TypeDef>, TypeDef, RuntimeException> returnTypeGetter,
                            LateInitFunction<List<TypeDef>, TypedExpr, CompilationException> body) {
        this(loc, pub, name, disambiguationIndex, numGenerics, isStatic, inline, owningType, numParams, paramNames, paramTypeGetter, returnTypeGetter, body, numGenerics == 0 ? null : new HashMap<>());
    }

    @Override
    public List<TypeDef> paramTypes() {
        if (numGenerics == 0)
            return paramTypeGetter.get(List.of());
        throw new IllegalStateException("Attempt to get param types of generic method? Bug in compiler, please report!");
    }

    @Override
    public TypeDef returnType() {
        if (numGenerics == 0)
            return returnTypeGetter.get(List.of());
        throw new IllegalStateException("Attempt to get return type of generic method? Bug in compiler, please report!");
    }

    @Override
    public TypedExpr constantFold(TypedMethodCall call) {
        if (isStatic)
            //throw new IllegalStateException("Calling non-static method statically? Bug in compiler, please report");
            return constantFold(new TypedStaticMethodCall(
                    call.loc(), call.receiver().type(), call.method(),
                    ListUtils.join(List.of(call.receiver()), call.args()),
                    call.type()
            ));
        //If the body is just a literal, then constant fold the method call into that literal
        //ACTUALLY NO, SINCE RECEIVER/ARGS COULD HAVE SIDE EFFECTS
//        if (body.tryGet(b -> b) instanceof TypedLiteral literalBody)
//            return literalBody;
        if (!inline) return call;
        //TODO: Add inlining
        return call;
    }

    @Override
    public TypedExpr constantFold(TypedStaticMethodCall call) {
        if (!isStatic) throw new IllegalStateException("Calling non-static method statically? Bug in compiler, please report");
        //If the body is just a literal, then constant fold the method call into that literal. (Also no args allowed, since those could have side effects)
        if (body.tryGet(List.of(), b -> b) instanceof TypedLiteral literalBody && paramNames.size() == 0)
            return literalBody;
        if (!inline) return call;
        //TODO: Add inlining
        return call;
    }

    //Compile this method into a CodeBlock and return it
    public CodeBlock compileToCodeBlock() throws CompilationException {
        CodeBlock block = new CodeBlock(this);
        if (!isStatic) {
            if (!isConstructor() || !owningType.isPlural()) //Don't give a "this" local to plural-type constructors
                block.env.declare(loc, "this", owningType); //"this" is first variable for non-static methods
        }

        for (int i = 0; i < paramNames.size(); i++)
            block.env.declare(loc, paramNames.get(i), paramTypeGetter.get(List.of()).get(i)); //Declare other params
        body.getAlreadyFilled(List.of()).compile(block, null); //Compile the body
        block.emit(new Return(this, returnTypeGetter.get(List.of()))); //Return result of the body
        return block;
    }

    @Override
    public boolean pub() {
        return pub; //Idk if the record one already overrides, so doing this to be sure
    }


    //Has this method def been instantiated with the given generics before?
    public boolean hasInstantiated(List<TypeDef> methodGenerics) {
        return instantiationCache.containsKey(methodGenerics);
    }
    public SnuggleMethodDef instantiate(List<TypeDef> methodGenerics) {
        return instantiationCache.computeIfAbsent(methodGenerics, m -> new SnuggleMethodDef(
                loc, pub, GenericStringUtil.instantiateName(name, methodGenerics), disambiguationIndex, 0, isStatic, inline, owningType, numParams, paramNames,
                new LateInitFunction<>(unused -> paramTypeGetter.get(methodGenerics)),
                new LateInitFunction<>(unused -> returnTypeGetter.get(methodGenerics)),
                new LateInitFunction<>(unused -> body.get(methodGenerics))
        ));
    }

    public String dedupName() {
        if (!owningType.isPlural() && isConstructor())
            return "<init>";
        String name = name();
        if (disambiguationIndex > 0)
            name += "$" + disambiguationIndex;
        return name;
    }

    @Override
    public void compileCall(boolean isSupercall, CodeBlock block, List<FieldDef> desiredFields, MethodVisitor jvm) {
        int instruction = Opcodes.INVOKEVIRTUAL; //Virtual by default
        if (isStatic() || owningType().isPlural())
            instruction = Opcodes.INVOKESTATIC; //Static methods, or methods on plural topLevelTypes, are static java-side
        else if (isSupercall || isConstructor())
            instruction = Opcodes.INVOKESPECIAL; //Super calls and constructor calls use InvokeSpecial

        //Invoke the instruction
        jvm.visitMethodInsn(instruction, owningType.runtimeName(), GenericStringUtil.mangleSlashes(dedupName()), getDescriptor(), false);
    }

    @Override
    public void checkCode() throws CompilationException {
        if (numGenerics == 0)
            body.get(List.of()); //Evaluate the lazy body, if no generics
        //If there are generics, nothing to be done here.
    }
}
