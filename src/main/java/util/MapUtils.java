package util;

import java.util.HashMap;
import java.util.Map;

public class MapUtils {

    public static <K, V1, V2, E extends Throwable> Map<K, V2> mapValues(Map<K, V1> map, ThrowingFunction<V1, V2, E> func) throws E {
        Map<K, V2> result = new HashMap<>();
        for (Map.Entry<K, V1> entry : map.entrySet())
            result.put(entry.getKey(), func.apply(entry.getValue()));
        return result;
    }

    public static <K1, K2, V, E extends Throwable> Map<K2, V> mapKeys(Map<K1, V> map, ThrowingFunction<K1, K2, E> func) throws E {
        Map<K2, V> result = new HashMap<>();
        for (Map.Entry<K1, V> entry : map.entrySet())
            result.put(func.apply(entry.getKey()), entry.getValue());
        return result;
    }

}
