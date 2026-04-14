package Project.command.TreeCommands;

import Project.command.Command;
import Project.model.ANode;
import javafx.scene.control.TreeView;

/**
 * A Command that has a Tree.
 */
public abstract class TreeCommand implements Command {

    protected TreeView<ANode> treeView;
    public TreeCommand(TreeView<ANode> treeView) {

        this.treeView = treeView;
    }


}
