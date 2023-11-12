package ast.passes;

import exceptions.compile_time.CompilationException;
import ast.type_resolved.ResolvedType;
import ast.type_resolved.def.type.TypeResolvedTypeDef;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.prog.TypeResolvedAST;
import ast.type_resolved.prog.TypeResolvedFile;
import exceptions.compile_time.GenericCountException;
import lexing.Loc;

import java.util.List;

/**
 * Verify that the generics used here have the correct
 * number of arguments.
 * For example, trying to use List<int, int> will cause an error.
 */
public class GenericVerifier {

    private final List<TypeResolvedTypeDef> startingTypeDefs;

    public static void verifyGenerics(TypeResolvedAST ast) throws CompilationException {
        new GenericVerifier(ast).verifyGenericArgCounts(ast);
    }

    private GenericVerifier(TypeResolvedAST ast) {
        this.startingTypeDefs = ast.typeDefs();
    }

    //Ensure there are no incorrect generic param counts (i.e. List<i32, i32>)
    private void verifyGenericArgCounts(TypeResolvedAST ast) throws CompilationException {
        //Verify all the topLevelTypes
        for (TypeResolvedTypeDef typeDef : ast.typeDefs())
            typeDef.verifyGenericCounts(this);
        //Verify the code
        for (TypeResolvedFile file : ast.files().values())
            for (TypeResolvedExpr expr : file.code())
                expr.verifyGenericArgCounts(this);
    }

    //Verify a single annotatedType. Helper method for implementors of verifyGenericArgCounts
    public void verifyType(ResolvedType type, Loc loc) throws CompilationException {
        if (!(type instanceof ResolvedType.Basic basic))
            return;

        TypeResolvedTypeDef def = startingTypeDefs.get(basic.index());
        int expectedGenerics = def.numGenerics();
        int givenGenerics = basic.generics().size();
        if (expectedGenerics != givenGenerics)
            throw new GenericCountException("Attempt to instantiate annotatedType " + def.name() + " with " + givenGenerics + " generics, but it expected " + expectedGenerics, loc);

        //Ensure the sub-generics are also okay
        for (ResolvedType generic : basic.generics())
            verifyType(generic, loc);
    }

}
