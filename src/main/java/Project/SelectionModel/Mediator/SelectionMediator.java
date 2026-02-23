package Project.SelectionModel.Mediator;

import Project.SelectionModel.SelectionGroup;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.util.Set;

/**
 * This class mediates between two SelectionGroups.
 * Used to add/remove/push the selection of one group onto the other.
 * @param <A> data type of Set() Selection of selectionGroupA
 * @param <B> data type of Set() Selection of selectionGroupB
 */
public abstract class SelectionMediator<A, B> {

    /**
     * Accept a selection of format of group A and transform it into the format required by group B.
     */
    public abstract Set<B> transformAselectionToBSelection(Set<A> selection);

    /**
     * Accept a selection of format of group B and transform it into the format required by group A.
     */
    public abstract Set<A> transformBSelectionToASelection(Set<B> selection);

    /**
     * Updates B with the selection of A.
     * @return items that were failed to be transferred.
     */
    public ObservableSet<B> pushAtoB() {
        Set<B> selectionA= transformAselectionToBSelection(selectionGroupA.getSelection());
        selectionGroupB.changeSelection(selectionA, true, false);
        return selectionGroupB.getFailedToSelect();
    }

    /**
     * Updates A with the selection of B.
     * @return items that were failed to be transferred.
     */
    public ObservableSet<A> pushBtoA() {
        Set<A> selectionB = transformBSelectionToASelection(selectionGroupB.getSelection());
        for (A a : selectionB) System.out.println(a.toString());
        selectionGroupA.changeSelection(selectionB, true, false);
        return selectionGroupA.getFailedToSelect();
    }

    /**
     * Add the selection of A to B.
     * @return items that were failed to be transferred.
     */
    public ObservableSet<B> addAtoB() {
        Set<B> selectionA= transformAselectionToBSelection(selectionGroupA.getSelection());
        selectionGroupB.changeSelection(selectionA, false, false);
        return selectionGroupB.getFailedToSelect();
    }

    /**
     * Add the selection of B to A.
     * @return items that were failed to be transferred.
     */
    public ObservableSet<A> addBtoA() {
        Set<A> selectionB = transformBSelectionToASelection(selectionGroupB.getSelection());
        selectionGroupA.changeSelection(selectionB, false, false);
        return selectionGroupA.getFailedToSelect();
    }

    /**
     * Remove the selection of A from B.
     * @return items that were failed to be transferred.
     */
    public ObservableSet<B> removeAfromB() {
        Set<B> selectionA = transformAselectionToBSelection(selectionGroupA.getSelection());
        selectionGroupB.changeSelection(selectionA, false, true);
        return selectionGroupB.getFailedToSelect();
    }

    /**
     * Remove the selection of B from A.
     * @return items that were failed to be transferred.
     */
    public ObservableSet<A> removeBfromA() {
        Set<A> selectionB = transformBSelectionToASelection(selectionGroupB.getSelection());
        selectionGroupA.changeSelection(selectionB, false, true);
        return selectionGroupA.getFailedToSelect();
    }


    protected SelectionGroup<A> selectionGroupA;
    protected SelectionGroup<B> selectionGroupB;

    public SelectionMediator(SelectionGroup<A> selectionGroupA, SelectionGroup<B> selectionGroupB) {
        this.selectionGroupA = selectionGroupA;
        this.selectionGroupB = selectionGroupB;
    }

}
