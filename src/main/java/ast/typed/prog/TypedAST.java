package ast.typed.prog;

import ast.typed.def.type.TypeDef;

import java.util.List;
import java.util.Map;

public record TypedAST(List<TypeDef> typeDefs, Map<String, TypedFile> files) {

}
