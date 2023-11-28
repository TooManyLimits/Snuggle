package util;

public class Mutable<T> {
    public T v;
    public Mutable() {
        v = null;
    }
    public Mutable(T e) {
        v = e;
    }
}
