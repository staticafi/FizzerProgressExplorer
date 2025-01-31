package fizzer;

import java.util.Objects;

public class LocationId {
    public final int id;
    public final int context;
    private final int hashCode_;

    public LocationId(int id_, int context_) {
        id = id_;
        context = context_;
        hashCode_ = Objects.hash(id, context);
    }

    public boolean equals(int id_, int context_) {
        return id == id_ && context == context_;            
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LocationId other = (LocationId) o;
        return equals(other.id, other.context);            
    }

    @Override
    public int hashCode() {
        return hashCode_;
    }
}
