package Project.SelectionModel.HasGroup;

import Project.SelectionModel.HasSelection;
import Project.SelectionModel.SelectionContainer;
import Project.SelectionModel.Mediator.SelectionMediator_Tree_3D;

public abstract class HasGroupContainer<T,V> extends SelectionContainer<T, V> {

    SelectionMediator_Tree_3D selectionMediator;

    public HasGroupContainer(HasSelection<T> hasSelection, SelectionMediator_Tree_3D selectionMediator) {
        super(hasSelection);
        this.selectionMediator = selectionMediator;
    }

}
