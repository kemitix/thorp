package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MapView<K, V> {
    private final Map<K, V> map;
    public static <K, V> MapView<K, V> empty(){
        return MapView.of(new HashMap<>());
    }
    public static <K, V> MapView<K, V> of(Map<K, V> map) {
        return new MapView<>(map);
    }
    public boolean contains(K key) {
        return map.containsKey(key);
    }
    public Optional<V> get(K key) {
        return Optional.ofNullable(map.get(key));
    }
    public Collection<K> keys() { return map.keySet(); }
    public Optional<Tuple<K, V>> collectFirst(BiFunction<K, V, Boolean> test) {
        return map.entrySet().stream()
                .filter(e -> test.apply(e.getKey(), e.getValue()))
                .findFirst()
                .map(e -> Tuple.create(e.getKey(), e.getValue()));
    }
    public Map<K, V> asMap() {
        return new HashMap<>(map);
    }
    public int size() {
        return map.size();
    }
}
