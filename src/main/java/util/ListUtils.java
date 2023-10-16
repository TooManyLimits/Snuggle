package util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListUtils {

    public static <T, R, E extends Throwable> R[] mapArray(T[] arr, Class<R> rClass, ThrowingFunction<T, R, E> func) throws E {
        R[] result = (R[]) Array.newInstance(rClass, arr.length);
        for (int i = 0; i < arr.length; i++)
            result[i] = func.apply(arr[i]);
        return result;
    }

    public static <T, R, E extends Throwable> List<R> map(List<T> list, ThrowingFunction<T, R, E> func) throws E {
        ArrayList<R> result = new ArrayList<>(list.size());
        for (T elem : list)
            result.add(func.apply(elem));
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
     * Join all lists into one
     */
    public static <T> List<T> join(List<List<T>> lists) {
        ArrayList<T> res = new ArrayList<>();
        for (List<T> list : lists)
            res.addAll(list);
        res.trimToSize();
        return res;
    }

}
