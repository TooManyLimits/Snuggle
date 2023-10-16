package ast.type_resolved;

import java.util.List;

/**
 * Essentially a "handle" to a typedef.
 * The replacement for a ParsedType, after the annotatedType resolution phase.
 */
public sealed interface ResolvedType {

    record Basic(int index, List<ResolvedType> generics) implements ResolvedType {

    }

    record Generic(int index, boolean isMethod) implements ResolvedType {

    }

}
