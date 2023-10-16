package ast.typed.def.type;

import compile.Compiler;
import exceptions.CompilationException;
import lexing.Loc;

public interface SnuggleTypeDef extends TypeDef {
    Loc loc();

    int index();

    //Compile this to a class file byte[],
    //Using the information available in the compiler
    byte[] compile(Compiler compiler) throws CompilationException;

}
