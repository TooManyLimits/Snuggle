package ast.typed;

import ast.passes.TypePool;

public sealed interface Type {

    boolean isSubtype(Type other, TypePool pool);
    String name(TypePool pool);


    record Basic(int index) implements Type {
        @Override
        public boolean isSubtype(Type other, TypePool pool) {
            return equals(other) || pool.getTypeDef(this).isSubtype(other, pool);
        }

        @Override
        public String name(TypePool pool) {
            return pool.getTypeDef(this).name();
        }
    }

    //Always a method generic, no bool needed
    record Generic(int index) implements Type {
        @Override
        public boolean isSubtype(Type other, TypePool pool) {
            throw new UnsupportedOperationException("Shouldn't be checking if generic is subtype - bug in compiler, please report");
        }

        @Override
        public String name(TypePool pool) {
            throw new UnsupportedOperationException("Cannot get generic's methodName - bug in compiler, please report");
        }
    }
}
