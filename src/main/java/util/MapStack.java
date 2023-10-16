package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class MapStack<K, V> {

    private final Stack<Map<K, V>> maps = new Stack<>();

    public MapStack() {
        maps.push(new HashMap<>());
    }

    public void push() { maps.push(new HashMap<>()); }

    //Returns the popped map
    public Map<K, V> pop() { return maps.pop(); }

    public void put(K key, V value) {
        maps.peek().put(key, value);
    }

    //Return the previous value associated with the key, or null if none
    //Operates only on the topmost map
    public V putIfAbsent(K key, V value) {
        return maps.peek().putIfAbsent(key, value);
    }

    public V get(K key) {
        for (int i = maps.size() - 1; i >= 0; i--) {
            V val = maps.get(i).get(key);
            if (val != null)
                return val;
        }
        return null;
    }

}
