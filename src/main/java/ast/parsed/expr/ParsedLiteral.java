package ast.parsed.expr;

import ast.passes.TypeResolver;
import ast.type_resolved.expr.TypeResolvedExpr;
import ast.type_resolved.expr.TypeResolvedLiteral;
import builtin_types.BuiltinType;
import builtin_types.types.BoolType;
import builtin_types.types.StringType;
import builtin_types.types.numbers.FloatLiteralType;
import builtin_types.types.numbers.FloatType;
import builtin_types.types.numbers.IntegerType;
import lexing.Loc;
import util.Fraction;
import util.IntLiteralData;

/**
 * ParsedLiteral value can be one of:
 * - IntLiteralData
 * TODO - FloatLiteralData
 * TODO - String
 * TODO - Boolean
 */
public record ParsedLiteral(Loc loc, Object value) implements ParsedExpr {

    @Override
    public TypeResolvedExpr resolve(TypeResolver resolver) {

        BuiltinType builtin;
        if (value instanceof Boolean) builtin = BoolType.INSTANCE;
        else if (value instanceof String) builtin = StringType.INSTANCE;
        else if (value instanceof IntLiteralData data) builtin = IntegerType.fromIntData(data);
        else if (value instanceof Fraction) builtin = FloatLiteralType.INSTANCE;
        else if (value instanceof Float) builtin = FloatType.F32;
        else if (value instanceof Double) builtin = FloatType.F64;
        else throw new IllegalStateException("Unrecognized literal value: " + value);

        return new TypeResolvedLiteral(loc, value, resolver.resolveBasicBuiltin(builtin));
    }
}
