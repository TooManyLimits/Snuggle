package ast.typed.def.type;

//Was this type generated from a TypeHead + some generics?
//Implemented by classes, structs, builtins.
//Tuples and function types have their own special handling
//instead.
public interface FromTypeHead {
    int getTypeHeadId();
}
