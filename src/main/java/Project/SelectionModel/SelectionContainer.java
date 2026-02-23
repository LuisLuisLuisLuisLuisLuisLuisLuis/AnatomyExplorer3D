package Project.SelectionModel;


import Project.model.ANode;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.util.Collection;
import java.util.Set;

/**
 * Class that wraps an Object that has a selection (HasSelection).
 * Is part of a larger SelectionGroup (of many SelectionContainers).
 * @param <groupSelectionType> The data type of the selection (Set) shared by the SelectionGroup.
 */

public abstract class SelectionContainer<selectionType, groupSelectionType> {

    /**
     * an object that has a selection.
     */
    protected HasSelection<selectionType> hasSelection;

    /**
     * the SelectionGroup this object is a part of.
     */
    protected SelectionGroup<groupSelectionType> selectionGroup;

    private boolean isPartOfGroup;


    public boolean isPartOfGroup() {return isPartOfGroup;}

    /**
     * set the group of this if this is not part of a group already
     */
    protected boolean setGroup(SelectionGroup<groupSelectionType> selectionGroup) {
        if (isPartOfGroup()) {
            //System.err.println("Container already part of group");
            return false;
        }
        this.selectionGroup = selectionGroup;
        isPartOfGroup = true;
        return true;
    }
    protected void removeFromGroup() {
        this.selectionGroup = new SelectionGroup<>();
        isPartOfGroup = false;
    }

    /**
     * am I currently being updated by the selectionGroup?
     */
    private boolean isBeingUpdated = false;

    protected void isBeingUpdated(boolean state) {isBeingUpdated = state;}
    protected boolean isBeingUpdated() {return isBeingUpdated;}

    private boolean isUpdatingGroup = false;    // i am updating my group
    protected boolean isUpdatingGroup() {return isUpdatingGroup;}
    protected void isUpdatingGroup(boolean state) {isUpdatingGroup = state;}

    private boolean noUpdating = false;
    protected void setNoUpdating(boolean state) {
        this.noUpdating = state;
    }

    /**
     * Update the selectionGroup using the selection of this class.
     */
    protected void updateGroup() {
        if (isPartOfGroup() && !isBeingUpdated && !noUpdating) {
            selectionGroup.changeSelection(getSelectionFormatted(), true, false);
        } else isUpdatingGroup(false);
    }


    /**
     * return the selection of this SelectionContainer in the format that the selectionGroup works with.
     */
    public abstract Set<groupSelectionType> getSelectionFormatted();

    /**
     * Set the selection of this, using the format used by the selectionGroup.
     * To be called by the selectionGroup.
     * @param groupSelection: the selection.
     * @return those items that were failed to be selected.
     */
    protected ObservableList<groupSelectionType> changeSelection(Set<groupSelectionType> groupSelection, boolean clear, boolean remove) {
        if (!isUpdatingGroup()) {
            isBeingUpdated(true);
            clearFailedList();
            if (clear) hasSelection.clearSelection();
            for (groupSelectionType groupItem : groupSelection) {
                Collection<selectionType> itemsToSelect = transformGroupItemToSelectionItem(groupItem);
                if (itemsToSelect == null) continue;
                for (selectionType item : itemsToSelect) {
                    if (item != null) {
                        System.out.println("container: " + item.toString());
                        if (!remove) hasSelection.select(item);
                        else hasSelection.unselect(item);
                    } else {
                        failedToSelect(groupItem);
                    }
                }
            }
            isBeingUpdated(false);
        } else {
            isUpdatingGroup(false);
            clearFailedList();
        }
        return getFailedToSelect();
    }


    protected abstract Collection<selectionType> transformGroupItemToSelectionItem(groupSelectionType groupItem);

    private final ObservableList<groupSelectionType> failedToSelect = FXCollections.observableArrayList();
    public ObservableList<groupSelectionType> getFailedToSelect() {
        return failedToSelect;
    }
    protected void failedToSelect(groupSelectionType item) {
        failedToSelect.add(item);
    }
    protected void clearFailedList() {
        failedToSelect.clear();
    }

    /**
     * @param hasSelection: an object that has a selection
     * @param selectionGroup: a SelectionGroup for this container to be part of
     */
    public SelectionContainer(HasSelection<selectionType> hasSelection, SelectionGroup<groupSelectionType> selectionGroup) {
        this.hasSelection = hasSelection;
        this.selectionGroup = selectionGroup;
        this.hasSelection.getSelection().addListener((ListChangeListener<? super selectionType>) e -> {
            isUpdatingGroup = true;
            updateGroup(); //if the selection changes, update the group
        });
        isPartOfGroup = true;
    }

    /**
     * Construct without giving a SelectionGroup.
     * @param hasSelection: an object that has a selection.
     */
    public SelectionContainer(HasSelection<selectionType> hasSelection) {
        this.hasSelection = hasSelection;
        this.hasSelection.getSelection().addListener((ListChangeListener<? super selectionType>) e -> {
            isUpdatingGroup = true;
            updateGroup(); //if the selection changes, update the group
        });
        isPartOfGroup = false;
    }
}