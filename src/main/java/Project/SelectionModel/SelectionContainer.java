package Project.SelectionModel;


import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

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
        this.selectionGroup = null;
        isPartOfGroup = false;
    }

    /**
     * am I currently being updated by the selectionGroup?
     */
    private boolean isBeingUpdated = false;
    protected boolean isBeingUpdated() {return isBeingUpdated;}
    protected void isBeingUpdated(boolean state) {isBeingUpdated = state;}


    private boolean isUpdatingGroup = false;    // i am updating my group
    protected boolean isUpdatingGroup() {return isUpdatingGroup;}
    protected void isUpdatingGroup(boolean state) {isUpdatingGroup = state;}    //TODO: ALWAYS FALSE correct?

    private boolean noUpdating = false;
    protected void setNoUpdating(boolean state) {
        this.noUpdating = state;
    }

    /**
     * Update the selectionGroup using the selection of this class.
     */
    public void updateGroup() {
        if (isPartOfGroup() && !isBeingUpdated() && !noUpdating) {
            isUpdatingGroup = true;
            selectionGroup.changeSelection(getSelectionFormatted(), true, false);
        } else isUpdatingGroup(false);
    }


    /**
     * return the selection of this SelectionContainer in the format that the selectionGroup works with.
     */
    public abstract Set<groupSelectionType> getSelectionFormatted();

    /**
     * Set the selection of this, using the format used by the selectionGroup.
     * ONLY to be called by the selectionGroup.
     * Does NOT update the selection group (because it is only called by the selection group).
     * @param groupSelection: the selection.
     * @return those items that were failed to be selected.
     */
    protected ObservableList<groupSelectionType> changeSelection(@NotNull Set<groupSelectionType> groupSelection, boolean clear, boolean remove) {
        long startTime = System.nanoTime();
        if (!isUpdatingGroup()) {
            isBeingUpdated(true);
            clearFailedList();
            if (clear) hasSelection.clearSelection();
            for (groupSelectionType groupItem : groupSelection) {
                Collection<selectionType> itemsToSelect = transformGroupItemToSelectionItem(groupItem);
                if (itemsToSelect == null) continue;
                for (selectionType item : itemsToSelect) {
                    if (item != null) {
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
        long endTime = System.nanoTime();
        long durationMillis = (endTime - startTime) / 1000000;
        System.out.println("changeSelection took " + durationMillis);

        return getFailedToSelect();
    }


    public abstract Collection<selectionType> transformGroupItemToSelectionItem(groupSelectionType groupItem);

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
    public SelectionContainer(HasSelection<selectionType> hasSelection, @NotNull SelectionGroup<groupSelectionType> selectionGroup) {
        this.hasSelection = hasSelection;
        this.selectionGroup = selectionGroup;
        selectionGroup.addSelectionContainer(this);
        this.hasSelection.getSelection().addListener((ListChangeListener<? super selectionType>) e -> {
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
            updateGroup(); //if the selection changes, update the group
        });
        isPartOfGroup = false;
    }

    public String getId() {return this.hasSelection.getId();}
}