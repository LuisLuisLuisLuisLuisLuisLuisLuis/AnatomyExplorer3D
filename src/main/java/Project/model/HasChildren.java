package Project.model;

import java.util.Collection;

public interface HasChildren<T> {
    public Collection<T> getChildren();
}
