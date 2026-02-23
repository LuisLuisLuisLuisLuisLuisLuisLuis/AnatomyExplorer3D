package Project.SelectionModel;

import javafx.collections.ObservableList;

import java.util.List;

/**
 * Wraps anything that has a selection and provides uniform access methods.
 * @param <T>
 */
public interface HasSelection<T> {
    ObservableList<T> getSelection();
    void setSelection(ObservableList<T> selection);
    void select(T item);
    void unselect(T item);
    void clearSelection();
    List<T> getAllItems();


}
