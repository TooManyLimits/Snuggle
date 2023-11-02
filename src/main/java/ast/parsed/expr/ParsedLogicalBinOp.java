package ast.parsed.expr;

import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedLogicalBinOp;
import exceptions.compile_time.CompilationException;
import lexing.Loc;

//and -> &&
//!and -> ||
public record ParsedLogicalBinOp(Loc loc, boolean and, ParsedExpr lhs, ParsedExpr rhs) implements ParsedExpr {
    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) throws CompilationException {
        return new TypeResolvedLogicalBinOp(loc, and, lhs.resolve(resolver), rhs.resolve(resolver));
    }
}
