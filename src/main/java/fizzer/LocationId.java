package fizzer;

public class LocationId implements Comparable<LocationId> {
    public final int id;

    public LocationId(int id_) {
        id = id_;
    }

    public boolean equals(int id_) {
        return id == id_;
    }

    @Override
    public int compareTo(LocationId id_) {
        return id - id_.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LocationId other = (LocationId) o;
        return equals(other.id);            
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }
}
