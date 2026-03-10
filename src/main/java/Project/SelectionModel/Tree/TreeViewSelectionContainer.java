package Project.SelectionModel.Tree;

import Project.SelectionModel.SelectionContainer;
import Project.SelectionModel.SelectionGroup;
import Project.model.ANode;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.util.*;

/**
 * Wraps a HasTreeViewSelection and allows communication with a SelectionGroup.
 */
public class TreeViewSelectionContainer extends SelectionContainer<TreeItem<ANode>, ANode> {

    @Override
    public Set<ANode> getSelectionFormatted() {
        ObservableList<TreeItem<ANode>> selectedItems = hasSelection.getSelection();
        Set<ANode> selectionFormatted = new HashSet<ANode>();
        for (TreeItem<ANode> treeItem : selectedItems) {
            if (treeItem == null) continue;

            selectionFormatted.add(treeItem.getValue());
        }
        return selectionFormatted;
    }

    /**
     * @return the set of ANodes of the given treeItems.
     */
    public Set<ANode> getSelectionFormatted(ObservableList<TreeItem<ANode>> selectedItems) {
        Set<ANode> selectionFormatted = new HashSet<>();
        for (TreeItem<ANode> treeItem : selectedItems) {
            if (treeItem == null) {continue;}

            selectionFormatted.add(treeItem.getValue());
        }
        return selectionFormatted;
    }



    @Override
    protected Collection<TreeItem<ANode>> transformGroupItemToSelectionItem(ANode groupItem) {
        LinkedList<TreeItem<ANode>> result = new LinkedList<>();
        result.add(quickAccessMap.getOrDefault(groupItem.conceptId(), null));
        return result;
    }


    private final HashMap<String, TreeItem<ANode>> quickAccessMap = new HashMap<>();

    public TreeViewSelectionContainer(HasTreeView<ANode> hasTreeView, SelectionGroup<ANode> selectionGroup) {
        super(hasTreeView, selectionGroup);
        setupQuickAccessMap();
    }


    /**
     * Set up the quickAccessMap.
     */
    private void setupQuickAccessMap() {
        for (TreeItem<ANode> treeItem : hasSelection.getAllItems()) {
            //maps concept id (instead of anode) to treeitem, so that the treeviews can be synced (their anodes may be equivalent but still different objects)
            quickAccessMap.put(treeItem.getValue().conceptId(), treeItem);
        }

    }
}
