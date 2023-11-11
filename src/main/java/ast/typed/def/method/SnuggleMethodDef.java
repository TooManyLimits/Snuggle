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
import util.LateInit;

import java.util.List;

public record SnuggleMethodDef(Loc loc, boolean pub, String name, int disambiguationIndex, int numGenerics, boolean isStatic, boolean inline, TypeDef owningType, List<String> paramNames, List<TypeDef> paramTypes, TypeDef returnType, LateInit<TypedExpr, CompilationException> body) implements MethodDef {

    @Override
    public TypedExpr constantFold(TypedMethodCall call) {
        if (isStatic) throw new IllegalStateException("Calling non-static method statically? Bug in compiler, please report");
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
        if (body.tryGet(b -> b) instanceof TypedLiteral literalBody && paramTypes.size() == 0)
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
            block.env.declare(loc, paramNames.get(i), paramTypes.get(i)); //Declare other params
        body.getAlreadyFilled().compile(block, null); //Compile the body
        block.emit(new Return(this, returnType.get())); //Return result of the body
        return block;
    }

    @Override
    public boolean pub() {
        return pub; //Idk if the record one already overrides, so doing this to be sure
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
        jvm.visitMethodInsn(instruction, owningType.name(), dedupName(), getDescriptor(), false);
    }

    @Override
    public void checkCode() throws CompilationException {
        body.get(); //Evaluate the lazy body
    }
}
