package ast.typed.expr;

import ast.ir.def.CodeBlock;
import ast.typed.def.field.FieldDef;
import ast.typed.def.type.TypeDef;
import ast.ir.helper.ScopeHelper;
import exceptions.compile_time.CompilationException;
import lexing.Loc;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record TypedFieldAccess(Loc loc, TypedExpr lhs, FieldDef field, TypeDef type) implements TypedExpr {

    @Override
    public void compile(CodeBlock block) {
        throw new IllegalStateException("Fields not re-implemented yet");
    }
}
