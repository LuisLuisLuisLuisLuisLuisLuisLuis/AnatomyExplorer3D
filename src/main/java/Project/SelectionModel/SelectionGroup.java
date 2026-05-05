package Project.SelectionModel;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A group of many SelectionContainers. Synchronizes the SelectionContainers to always share the same selection if possible.
 * @param <groupSelection>
 */

public class SelectionGroup<groupSelection> {

    private static final Logger logger = Logger.getLogger(SelectionGroup.class.getName());

    //id
    private final String id;

    public SelectionGroup(String id) {this.id = id;}

    public String getId() {return id;}

    /**
     * All the SelectionContainers in this group.
     */
    private List<SelectionContainer> selectionContainers = new LinkedList<>();
    /**
     * The global selection of this group.
     */
    private ObservableSet<groupSelection> selection = FXCollections.observableSet();

    /**
     * @return the selection of this group.
     */
    public ObservableSet<groupSelection> getSelection() {
        return selection;
    }

    /**
     * Fires when the selection changed, but after all changes have been completed.
     */
    private Runnable runnable;

    public void setRunnable(Runnable runnable) {this.runnable = runnable;}

    private LinkedList<groupSelection> change = new LinkedList<>();
    public LinkedList<groupSelection> getChange() {return change;}
    private boolean wasAdded = false;
    public boolean wasAdded() {return wasAdded;}
    private boolean wasCleared = false;
    public boolean wasCleared() {return wasCleared;}

    /**
     * SelectionContainers may have items that are exclusive to them. Thus, other SelectionContainers can't select those.
     */
    private ObservableSet<groupSelection> failedToSelect = FXCollections.observableSet();
    public ObservableSet<groupSelection> getFailedToSelect() {return failedToSelect;}

    private boolean noUpdating = false;

    /** If set, this group's selection will not be able to be updated by calling changeSelection(). Further, SelectionContainers will not attempt to update this group.*/
    public void setNoUpdating(boolean value) {
        this.noUpdating = value;
        setNoUpdatingInContainers();
    }

    private void setNoUpdatingInContainers() {
        for (SelectionContainer selectionContainer: selectionContainers) {
            selectionContainer.setNoUpdating(noUpdating);
        }
    }

    /**
     * Change the selection of this group with the given selection.
     * @param selection: set of items
     * @param clear: to clear the current selection before adding the new selection
     * @param remove: to add or to remove the given items from the selection.
     */
    public void changeSelection(Set<groupSelection> selection, boolean clear, boolean remove) {
        this.failedToSelect.clear();
        if (noUpdating) return;
        logger.log(Level.CONFIG, "clear: " + clear + ", remove: " + remove + ". selection: " + selection);

        LinkedList<groupSelection> change = new LinkedList<>(selection); // remember the changes in case someone asks
        if (clear) {
            this.selection.clear();
        }

        if (!remove) this.selection.addAll(selection);
        else this.selection.removeAll(selection);


        boolean firstContainer = true;
        int counter = 0;
        boolean updatedFromWithinGroup = false; // was this update caused by one of my selectionContainers or from outside?

        for (SelectionContainer selectionContainer : selectionContainers) {
            //commented out code is old code that was buggy (i dont know why)
//            if (!selectionContainer.isUpdatingGroup()) counter++; //count containers that are to be updated, i.e. are not the ones causing this update
//            else updatedFromWithinGroup = true;
//            Set<groupSelection> failedToSelectByContainer = new HashSet<>(selectionContainer.changeSelection(selection, clear, remove)); //items that were failed to be selected by that container
//
//            if (this.failedToSelect.isEmpty()){
//                if (counter == 1) this.failedToSelect.addAll(failedToSelectByContainer);
//            }
//            else {
//                if (failedToSelectByContainer.isEmpty() && !selectionContainer.isUpdatingGroup()) this.failedToSelect.clear();
//                else {//the container updating this group will always return an empty failedToSelect list
//                    //was busy debugging here because the if inside the for throws exc. only since i mapped internal nodes to fileIDs in SelectionMediatorTree_3D
//                    for (groupSelection item : this.failedToSelect) {
//                        if (!failedToSelectByContainer.contains(item)) this.failedToSelect.remove(item);
//                    }
//                }
//            }
            //update container and store items that were failed to be selected by that container
            Set<groupSelection> failedToSelectByContainer = new HashSet<>(selectionContainer.changeSelection(selection, clear, remove));
            /*
            about the failedToSelect:
            I want to know if the new selection for this group contains any items that cannot be selected by any of its containers.
            - this is ONLY possible when updated from outside the group as the container that initiates the update can only update
            with items that it can select.
            -> When could this occur?
            What's the use? This is currently unused information. I was planning to print this info to the user.
             */
            if (!selectionContainer.isUpdatingGroup()) {
                if (firstContainer) {
                    this.failedToSelect.addAll(failedToSelectByContainer);
                    firstContainer = false;
                }
                this.failedToSelect.removeIf(item -> !failedToSelectByContainer.contains(item));
            } else updatedFromWithinGroup = true;

        }
        if (!updatedFromWithinGroup) this.selection.removeAll(this.failedToSelect); // so this group doesnt store stuff that none of its containers can select
        else this.failedToSelect.clear();

        change.removeAll(this.failedToSelect);
        storeLatestChange(change, clear, remove);

        if (this.runnable != null) this.runnable.run();
    }

    private void storeLatestChange(LinkedList<groupSelection> change, boolean clear, boolean remove) {
        this.wasCleared = clear;
        this.wasAdded = !remove;
        this.change = change;
    }

    /**
     * Add a SelectionContainer to this group.
     * @param selectionContainer
     */
    public void addSelectionContainer(SelectionContainer selectionContainer) {
        if (selectionContainers.contains(selectionContainer)) return;
        selectionContainers.add(selectionContainer);
        selectionContainer.setGroup(this);
        //instantly set the selection of the newly added selectionContainer here
        selectionContainer.changeSelection(this.selection, true, false);
    }

    /**
     * Remove a SelectionContainer from this group.
     * @param selectionContainer
     */
    public void removeSelectionContainer(SelectionContainer selectionContainer) {
        selectionContainers.remove(selectionContainer);
        selectionContainer.removeFromGroup();
        //System.out.println(selectionContainers.size() + " containers left in this group");
    }

}
