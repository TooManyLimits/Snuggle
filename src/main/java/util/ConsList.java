package util;

import util.throwing_interfaces.ThrowingBiFunction;
import util.throwing_interfaces.ThrowingFunction;

import java.util.Objects;

public sealed interface ConsList<T> {

    record Nil<T>() implements ConsList<T> {
        public Nil() {
            if (INSTANCE != null)
                throw new IllegalStateException("Cannot construct nil. Use the .nil() static method instead");
        }
        private static final Nil INSTANCE = new Nil<>();

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    record Cons<T>(T elem, ConsList<T> rest) implements ConsList<T> {
        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Cons<?> otherCons) &&
                    Objects.equals(elem, otherCons.elem) &&
                    rest.equals(otherCons.rest);
        }

        @Override
        public int hashCode() {
            return elem.hashCode() * 31 + rest.hashCode();
        }
    }

    static <T> ConsList<T> nil() {
        return Nil.INSTANCE;
    }

    static <T> ConsList<T> cons(T elem, ConsList<T> rest) {
        return new Cons<>(elem, rest);
    }

    static <T> ConsList<T> of(T... elems) {
        ConsList<T> current = nil();
        for (int i = elems.length - 1; i >= 0; i--)
            current = cons(elems[i], current);
        return current;
    }

    default <R, E extends Throwable> R match(ThrowingFunction<Nil<T>, R, E> nilCase, ThrowingBiFunction<T, ConsList<T>, R, E> consCase) throws E {
        if (this instanceof Cons<T> consThis)
            return consCase.apply(consThis.elem, consThis.rest);
        return nilCase.apply(((Nil<T>) this));
    }

    default <R, E extends Throwable> ConsList<R> map(ThrowingFunction<T, R, E> func) throws E {
        if (this instanceof ConsList.Cons<T> consThis)
            return cons(func.apply(consThis.elem), consThis.rest.map(func));
        return nil();
    }

    default <T2, R, E extends Throwable> ConsList<R> map2(ThrowingBiFunction<T, T2, R, E> func, ConsList<T2> otherList) throws E {
        if (this instanceof ConsList.Cons<T> consThis && otherList instanceof ConsList.Cons<T2> consOther)
            return cons(func.apply(consThis.elem, consOther.elem), consThis.rest.map2(func, consOther.rest));
        else if (this == Nil.INSTANCE && otherList == Nil.INSTANCE)
            return nil();
        throw new IllegalArgumentException("Expected lists to be the same length?");
    }

    default <E extends Throwable> ConsList<T> filter(ThrowingFunction<T, Boolean, E> predicate) throws E {
        if (this instanceof ConsList.Cons<T> consThis)
            return predicate.apply(consThis.elem) ? cons(consThis.elem, consThis.rest.filter(predicate)) : consThis.rest.filter(predicate);
        return nil();
    }

    default <E extends Throwable> T find(ThrowingFunction<T, Boolean, E> predicate) throws E {
        if (this instanceof ConsList.Cons<T> consThis)
            return predicate.apply(consThis.elem) ? consThis.elem : consThis.rest.find(predicate);
        return null;
    }



}
