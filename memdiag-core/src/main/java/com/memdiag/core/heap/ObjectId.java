package com.memdiag.core.heap;

import java.util.Objects;

public class ObjectId {
    private final long id;

    public ObjectId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectId objectId = (ObjectId) o;
        return id == objectId.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ObjectId{" + id + "}";
    }
}
