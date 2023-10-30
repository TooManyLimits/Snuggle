package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.vars.LoadLocal;
import ast.ir.instruction.vars.StoreLocal;
import ast.typed.def.type.TypeDef;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

public record TypedDeclaration(Loc loc, String name, TypeDef type, TypedExpr rhs) implements TypedExpr {

    //Largely taken from TypedAssignment
    @Override
    public void compile(CodeBlock code, DesiredFieldNode desiredFields) throws CompilationException {
        //Declare and calculate offset
        int index = code.env.declare(loc, name, type); //Get the index
        TypedAssignment.DefIndexPair desiredIndex = TypedAssignment.getDesiredIndexOffset(type, index, desiredFields); //Get the desired index/type
        //Emit
        rhs.compile(code, null); //First compile the rhs
        code.emit(new StoreLocal(index, type.get())); //Store
        code.emit(new LoadLocal(desiredIndex.index(), desiredIndex.def())); //Then load the desired
    }
}
