package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.misc.LineNumber;
import ast.ir.instruction.stack.Push;
import ast.ir.instruction.vars.LoadThis;
import ast.ir.instruction.objects.MethodCall;
import ast.typed.def.field.FieldDef;
import ast.typed.def.field.SnuggleFieldDef;
import ast.typed.def.method.MethodDef;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

import java.util.List;

public record TypedSuperMethodCall(TypeDef.InstantiationStackFrame cause, Loc loc, TypeDef currentType, TypeDef receiverType, MethodDef method, List<TypedExpr> args, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock code, DesiredFieldNode desiredFields) throws CompilationException {
        code.emit(new LoadThis(receiverType));
        for (TypedExpr arg : args)
            arg.compile(code, null);
        code.emit(new LineNumber(loc.startLine()));
        code.emit(new MethodCall(true, method, DesiredFieldNode.toList(desiredFields)));

        if (method.isConstructor() && !method.owningType().isPlural()) {
            //If this is a constructor call, super() AKA super.new(), then do all the field initializers
            for (FieldDef f : currentType.nonStaticFields()) {
                //For each field that has an initializer:
                if (f instanceof SnuggleFieldDef sf && sf.initializer() != null) {
                    //compile something like "this.f = f.initializer()"
                    new TypedAssignment(cause, loc,
                        new TypedFieldAccess(loc, new TypedVariable(loc, "this", currentType), f, f.type()),
                        sf.initializer().getAlreadyFilled(),
                        sf.initializer().getAlreadyFilled().type()
                    ).compile(code, desiredFields);
                }
            }

//            code.emit(new Push(cause, loc, Unit.INSTANCE, type));
        }

    }
}
