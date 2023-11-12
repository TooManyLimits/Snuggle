package util;

import util.throwing_interfaces.ThrowingBiFunction;
import util.throwing_interfaces.ThrowingConsumer;
import util.throwing_interfaces.ThrowingFunction;

import java.lang.reflect.Array;
import java.util.*;

public class ListUtils {

    public static <T, R, E extends Throwable> R[] mapArray(T[] arr, Class<R> rClass, ThrowingFunction<T, R, E> func) throws E {
        R[] result = (R[]) Array.newInstance(rClass, arr.length);
        for (int i = 0; i < arr.length; i++)
            result[i] = func.apply(arr[i]);
        return result;
    }

    public static <T, E extends Throwable> void iterBackwards(List<T> list, ThrowingConsumer<T, E> func) throws E {
        for (int i = list.size() - 1; i >= 0; i--)
            func.accept(list.get(i));
    }

    public static <T, R, E extends Throwable> List<R> map(List<T> list, ThrowingFunction<T, R, E> func) throws E {
        ArrayList<R> result = new ArrayList<>(list.size());
        for (T elem : list)
            result.add(func.apply(elem));
        return result;
    }

    public static <R, E extends Throwable> List<R> generate(int size, ThrowingFunction<Integer, R, E> func) throws E {
        ArrayList<R> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            result.add(func.apply(i));
        return result;
    }

    public static <T, R, E extends Throwable> List<R> mapIndexed(List<T> list, ThrowingBiFunction<T, Integer, R, E> func) throws E {
        ArrayList<R> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++)
            result.add(func.apply(list.get(i), i));
        return result;
    }

    public static <T, E extends Throwable> List<T> filter(List<T> list, ThrowingFunction<T, Boolean, E> func) throws E {
        ArrayList<T> result = new ArrayList<>();
        for (T elem : list)
            if (func.apply(elem))
                result.add(elem);
        result.trimToSize();
        return result;
    }

    public static <T, E extends Throwable> T find(List<T> list, ThrowingFunction<T, Boolean, E> func) throws E {
        for (T elem : list)
            if (func.apply(elem))
                return elem;
        return null;
    }

    public static <T> void setExpand(List<T> list, int index, T element) {
        while (list.size() <= index)
            list.add(null);
        list.set(index, element);
    }

    public static <T, E extends Throwable> void sort(List<T> list, ThrowingBiFunction<T, T, Integer, E> comparator, Class<E> eClass) throws E {
        try {
            list.sort((a, b) -> {
                try {
                    return comparator.apply(a, b);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException ex) {
            if (eClass.isInstance(ex.getCause()))
                throw (E) ex.getCause();
            throw ex;
        }
    }

    //Sometimes, java's List sort just isn't good enough.
    public static <T, E extends Throwable> void insertionSort(List<T> list, ThrowingBiFunction<T, T, Integer, E> comparator) throws E {
        List<T> output = new ArrayList<>(list.size());
        outer:
        for (T elem : list) {
            for (int i = 0; i < output.size(); i++) {
                int comparison = comparator.apply(output.get(i), elem);
                if (comparison > 0) { //the element that's currently in output[i] MUST come after elem. So insert it here.
                    output.add(i, elem);
                    continue outer;
                }
            }
            output.add(elem);
        }
        if (output.size() != list.size())
            throw new IllegalStateException("Failure in sort algorithm, bug in compiler, please report!");
        for (int i = 0; i < list.size(); i++)
            list.set(i, output.get(i));
    }


    /**
     * Generate a map from a given list, where the keys are chosen by the given function.
     * If two elements of the list share the same key, no error will occur, and the one
     * coming first in the list will be chosen.
     * If the func returns null, that element will not be put in the map.
     */
    public static <K, V, E extends Throwable> Map<K, V> indexBy(List<V> list, ThrowingFunction<V, K, E> func) throws E {
        Map<K, V> result = new HashMap<>();
        for (V elem : list) {
            K key = func.apply(elem);
            if (key != null)
                result.putIfAbsent(func.apply(elem), elem);
        }

        return result;
    }

    /**
     * Join all into one
     */
    public static <T> List<T> join(List<List<T>> lists) {
        ArrayList<T> res = new ArrayList<>();
        for (List<T> list : lists)
            res.addAll(list);
        res.trimToSize();
        return res;
    }

    public static <T> List<T> flatten(List<Collection<T>> collections) {
        ArrayList<T> res = new ArrayList<>();
        for (Collection<T> collection : collections)
            res.addAll(collection);
        res.trimToSize();
        return res;
    }

    @SafeVarargs
    public static <T> List<T> join(List<T>... lists) {
        return join(List.of(lists));
    }

}
