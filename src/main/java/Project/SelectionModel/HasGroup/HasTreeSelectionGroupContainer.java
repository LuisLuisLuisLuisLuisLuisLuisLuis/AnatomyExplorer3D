package Project.SelectionModel.HasGroup;

import Project.SelectionModel.HasSelection;
import Project.SelectionModel.Mediator.SelectionMediator_Tree_3D;
import Project.model.ANode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class HasTreeSelectionGroupContainer extends HasGroupContainer<ANode, String>{


    public HasTreeSelectionGroupContainer(HasSelection<ANode> hasSelection, SelectionMediator_Tree_3D selectionMediator) {
        super(hasSelection, selectionMediator);
    }

    @Override
    public Set<String> getSelectionFormatted() {
        //System.out.println("get selection formatted:");
        HashSet<ANode> set = new HashSet<>();
        set.addAll(hasSelection.getSelection());
        for (ANode s : set) System.out.println(s);
        return selectionMediator.transformAselectionToBSelection(set);
    }


    @Override
    public Collection<ANode> transformGroupItemToSelectionItem(String groupItem) {
        //System.out.println("tranform group item to selection item:" + selectionMediator.fileIDtoName(groupItem));
        return selectionMediator.fileIDtoANode(groupItem);
    }
}
