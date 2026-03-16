package Project.SelectionModel.HasGroup;

import Project.SelectionModel.HasSelection;
import Project.SelectionModel.SelectionGroup;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The point of this class (and the related Container) is to make SelectionGroups fit into SelectionContainers so they can be synchronized with other
 * selectionGroups without having to reinvent the wheel which is already implemented in SelectionContainer.
 * @param <groupSelection>
 */
public class HasGroup<groupSelection> implements HasSelection<groupSelection> {

    private SelectionGroup<groupSelection> selectionGroup;

    @Override
    public String getId() {
        return selectionGroup.getId();
    }

    public HasGroup(SelectionGroup selectionGroup) {
        this.selectionGroup = selectionGroup;

        selectionGroup.setRunnable(() -> {
            if (selectionGroup.wasCleared()) selection.clear();
            if (selectionGroup.wasAdded()) selection.addAll(selectionGroup.getChange());
            else selection.removeAll(selectionGroup.getChange());
        });
    }

    private ObservableList<groupSelection> selection = FXCollections.observableArrayList();

    @Override
    public ObservableList<groupSelection> getSelection() {
        return selection;
    }

    @Override
    public void setSelection(List<groupSelection> selection) {
        selectionGroup.changeSelection(makeSet(selection), true, false);
    }

    @Override
    public void select(groupSelection item) {
        selectionGroup.changeSelection(makeSet(item), false, false);
    }

    @Override
    public void unselect(groupSelection item) {
        selectionGroup.changeSelection(makeSet(item), false, true);
    }

    @Override
    public void clearSelection() {
        selectionGroup.changeSelection(new HashSet<>(), true, false);
    }

    @Override
    public List<groupSelection> getAllItems() {
        return getSelection();
    }

    private Set<groupSelection> makeSet(groupSelection item) {
        Set<groupSelection> set = new HashSet<>();
        set.add(item);
        return set;
    }
    private Set<groupSelection> makeSet(List<groupSelection> list) {
        Set<groupSelection> set = new HashSet<>();
        set.addAll(list);
        return set;
    }

}
