package ast.type_resolved.prog;

import ast.type_resolved.def.type.TypeResolvedTypeDef;
import builtin_types.BuiltinType;
import builtin_types.reflect.ReflectedBuiltin;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * At this stage, we have a bunch of TypeResolvedTypeDef information.
 * Additionally, we have each of the annotatedType-resolved files.
 * Finally, we also remember the ids for the builtin topLevelTypes.
 */
public record TypeResolvedAST(
        List<TypeResolvedTypeDef> typeDefs,
        Map<String, TypeResolvedFile> files,
        IdentityHashMap<BuiltinType, Integer> builtinIds,
        IdentityHashMap<Class<?>, ReflectedBuiltin> reflectedBuiltins
) {
    
}
