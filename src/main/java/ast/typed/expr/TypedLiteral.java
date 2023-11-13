package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.ir.instruction.stack.Push;
import ast.typed.def.type.BuiltinTypeDef;
import ast.typed.def.type.TypeDef;
import builtin_types.types.numbers.IntegerType;
import exceptions.compile_time.CompilationException;
import exceptions.compile_time.TypeCheckingException;
import lexing.Loc;

import java.math.BigInteger;
import java.util.Set;

public record TypedLiteral(TypeDef.InstantiationStackFrame cause, Loc loc, Object obj, TypeDef type) implements TypedExpr {

    @Override
    public void findAllThisFieldAccesses(Set<String> setToFill) {

    }

    /**
     * Used in the circumstance of type() being a non-storable type (like a literal),
     * when a concrete type is expected. Literals cannot exist at runtime, so they need to be
     * "pulled up" into their supertype in order for them to be compiled. For example:
     * - IntLiteral -> i32
     * - FloatLiteral -> f64
     * - StringLiteral -> String
     * Assume that this.type() is a subtype of expected; checks have already been made.
     */
    public TypedLiteral pullTypeUpwards(TypeDef expected) {
        return new TypedLiteral(cause, loc, obj, expected);
    }

    public void compile(CodeBlock code, DesiredFieldNode desiredFields) throws CompilationException {
        code.emit(new Push(cause, loc, obj, type));
    }

}