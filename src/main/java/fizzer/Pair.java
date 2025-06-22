package fizzer;

public class Pair<K, V> {
    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return (key == null ? pair.key == null : key.equals(pair.key)) &&
               (value == null ? pair.value == null : value.equals(pair.value));
    }

    @Override
    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) ^
               (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return "Pair{" + key + ", " + value + "}";
    }
}
