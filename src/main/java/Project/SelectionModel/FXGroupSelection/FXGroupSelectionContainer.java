package Project.SelectionModel.FXGroupSelection;

import Project.SelectionModel.SelectionContainer;
import Project.SelectionModel.SelectionGroup;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;

import java.util.*;

/**
 * Wraps a HasFXGroupSelection and allows communication with a SelectionGroup.
 */

public class FXGroupSelectionContainer extends SelectionContainer<MeshView, String> {

    /**
     * @return the selection as a list of IDs of every node.
     */
    @Override
    public Set<String> getSelectionFormatted() {
        ObservableList<MeshView> selectedItems = hasSelection.getSelection();
        Set<String> selectionFormatted = new HashSet<>();
        for (Node node : selectedItems) {
            selectionFormatted.add(node.getId());
        }
        return selectionFormatted;
    }


    @Override
    public Collection<MeshView> transformGroupItemToSelectionItem(String groupItem) {
        LinkedList<MeshView> result = new LinkedList<>();
        result.add(quickAccessMap.getOrDefault(groupItem, null));
        return result;
    }

    /**
     * Mapping of Node ID to Node for all nodes currently in the Group (synchronized).
     */
    private final HashMap<String, MeshView> quickAccessMap = new HashMap<>();

    public FXGroupSelectionContainer(HasFXGroupSelection hasFXGroupSelection) {
        super(hasFXGroupSelection);

        //making the quickaccessmap synchronized -> it always knows what nodes are available in the Group and thus available for selection.
        hasFXGroupSelection.getAllItemsObservable().addListener(new ListChangeListener<MeshView>() {
            @Override
            public void onChanged(Change<? extends MeshView> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        for (MeshView node : c.getAddedSubList()) {
                            quickAccessMap.put(node.getId(), node);
                        }
                    } else if (c.wasRemoved()) {
                        for (MeshView node : c.getRemoved()) {
                            quickAccessMap.remove(node.getId());
                        }
                    }
                }
            }
        });
    }

    public FXGroupSelectionContainer(HasFXGroupSelection hasFXGroup, SelectionGroup<String> selectionGroup) {
        super(hasFXGroup, selectionGroup);

        //making the quickaccessmap synchronized -> it always knows what nodes are available in the Group and thus available for selection.
        hasFXGroup.getAllItemsObservable().addListener(new ListChangeListener<MeshView>() {
            @Override
            public void onChanged(Change<? extends MeshView> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        for (MeshView node : c.getAddedSubList()) {
                            quickAccessMap.put(node.getId(), node);
                        }
                    } else if (c.wasRemoved()) {
                        for (MeshView node : c.getRemoved()) {
                            quickAccessMap.remove(node.getId());
                        }
                    }
                }
            }
        });
    }

}
