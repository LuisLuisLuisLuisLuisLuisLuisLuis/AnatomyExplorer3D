package Project.window;

import Project.AI.QnA;
import Project.model.ANode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MainWindowController {

    @FXML
    private CheckMenuItem enableBP3DPartsCheckmenu;
    @FXML
    private MenuItem loadHierarchyMenu;
    @FXML
    private CheckMenuItem loadBP3DV3checkMenu;
    @FXML
    private Button tiltClockwButton;
    @FXML
    private Button tiltAntiClButton;
    @FXML
    private MenuItem cutSelectedMenuItem;
    @FXML
    private MenuItem cutAllMenuItem;
    @FXML
    private SplitMenuButton cutSelectionButton;
    @FXML
    private Label botLabelDrawCount;
    @FXML
    private Button cutButt;
    @FXML
    private Button drawEverythingButton;
    @FXML
    private TextField fileIDTextfield;
    @FXML
    private MenuItem menuSaveTree;
    @FXML
    private TreeView<ANode> dumpTreeView;
    @FXML
    private MenuItem menuGuide;
    @FXML
    private ToolBar searchToolBar;
    @FXML
    private CheckMenuItem fullScreenCheck;
    @FXML
    private CheckBox selectionSynchCheck;
    @FXML
    private Button infoButton;
    @FXML
    private MenuBar topMenuBar;
    @FXML
    private CheckMenuItem darkModeMenuItem;
    @FXML
    private CheckBox showAICheck;
    @FXML
    private TableView<QnA> aiHistoryTable;
    @FXML
    private VBox aiButtonBox;
    @FXML
    private Button askAIButton;
    @FXML
    private Button aiHistoryButton;
    @FXML
    private TextArea aiPromptTextArea;
    @FXML
    private CheckBox regexCheck;
    @FXML
    private Button explodeButton;

    public Button getApplyColorButton() {
        return applyColorButton;
    }

    @FXML
    private Button applyColorButton;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private Button redoButton;
    @FXML
    private Button undoButton;
    @FXML
    private Button removeObjButton;

    @FXML
    private Button unselectAll3DButton;
    @FXML
    private Button selectAll3DButton;
    @FXML
    private Button showInTreeButton;
    @FXML
    private Button selectIn3DButton;


    @FXML
    private Button removeFrom3DButton;
    @FXML
    private Button drawIn3DButton;
    @FXML
    private Button zoomInButton;

    @FXML
    private Button zoomOutButton;

    @FXML
    private Label botLabel_3D;

    @FXML
    private Label botLabel_Tree;

    @FXML
    private TreeView<ANode> isATreeView;

    @FXML
    private MenuItem menuAbout;

    @FXML
    private MenuItem menuAddAxes;

    @FXML
    private MenuItem menuClearView;

    @FXML
    private MenuItem menuResetView;

    @FXML
    private MenuItem menuClose;



    @FXML
    private MenuItem menuRmAxes;

    @FXML
    private TreeView<ANode> partOfTreeView;

    @FXML
    private Pane threeDPane;

    @FXML
    private Button treeClearSelectedButton;

    @FXML
    private Button treeCollapseButton;

    @FXML
    private Button treeDrawSelectedButton;

    @FXML
    private Button treeExpandButton;

    @FXML
    private Button treeSearchAllButton;

    @FXML
    private TextField treeSearchField;

    @FXML
    private Button treeSearchNextButton;

    @FXML
    private Button treeSearchPrevButton;

    @FXML
    private Button treeSelectAllButton;

    @FXML
    private Button treeSelectNoneButton;

    @FXML
    private TabPane treeTabPane;

    @FXML
    private Button moveDownButton;

    @FXML
    private Button moveLeftButton;

    @FXML
    private Button moveRightButton;

    @FXML
    private Button moveUpButton;

    @FXML
    private Button resetViewButton;

    @FXML
    private Button rotDownButton;

    @FXML
    private Button rotLeftButton;

    @FXML
    private Button rotRightButton;

    @FXML
    private Button rotUpButton;

    @FXML
    private Button clearViewButton;

    public Button getRedoButton() {
        return redoButton;
    }

    public Button getUndoButton() {
        return undoButton;
    }

    public Button getUnselectAll3DButton() {
        return unselectAll3DButton;
    }

    public Button getSelectAll3DButton() {
        return selectAll3DButton;
    }

    public Button getShowInTreeButton() {
        return showInTreeButton;
    }

    public Button getSelectIn3DButton() {
        return selectIn3DButton;
    }

    public Button getRemoveFrom3DButton() {
        return removeFrom3DButton;
    }

    public Button getDrawIn3DButton() {
        return drawIn3DButton;
    }

    public Button getMoveDownButton() {
        return moveDownButton;
    }

    public Button getMoveLeftButton() {
        return moveLeftButton;
    }

    public Button getMoveRightButton() {
        return moveRightButton;
    }

    public Button getMoveUpButton() {
        return moveUpButton;
    }

    public Button getResetViewButton() {
        return resetViewButton;
    }

    public Button getRotDownButton() {
        return rotDownButton;
    }

    public Button getRotLeftButton() {
        return rotLeftButton;
    }

    public Button getRotRightButton() {
        return rotRightButton;
    }

    public Button getRotUpButton() {
        return rotUpButton;
    }

    public Button getClearViewButton() {
        return clearViewButton;
    }

    public Label getBotLabel_Tree() {
        return botLabel_Tree;
    }

    public Label getBotLabel_3D() {return botLabel_3D;}

    public TreeView<ANode> getIsATreeView() {
        return isATreeView;
    }

    public MenuItem getMenuAbout() {
        return menuAbout;
    }

    public MenuItem getMenuAddAxes() {
        return menuAddAxes;
    }

    public MenuItem getMenuClearView() {
        return menuClearView;
    }

    public MenuItem getMenuResetView() {return menuResetView;}

    public MenuItem getMenuClose() {
        return menuClose;
    }

    public MenuItem getMenuRmAxes() {
        return menuRmAxes;
    }

    public TreeView<ANode> getPartOfTreeView() {
        return partOfTreeView;
    }

    public Pane getThreeDPane() {
        return threeDPane;
    }

    public Button getTreeClearSelectedButton() {
        return treeClearSelectedButton;
    }

    public Button getTreeCollapseButton() {
        return treeCollapseButton;
    }

    public Button getTreeDrawSelectedButton() {
        return treeDrawSelectedButton;
    }

    public Button getTreeExpandButton() {
        return treeExpandButton;
    }

    public Button getTreeSearchAllButton() {
        return treeSearchAllButton;
    }

    public TextField getTreeSearchField() {
        return treeSearchField;
    }

    public Button getTreeSearchNextButton() {
        return treeSearchNextButton;
    }

    public Button getTreeSearchPrevButton() {
        return treeSearchPrevButton;
    }

    public Button getTreeSelectAllButton() {
        return treeSelectAllButton;
    }

    public Button getTreeSelectNoneButton() {return treeSelectNoneButton;}

    public TabPane getTreeTabPane() {return treeTabPane;}

    public Button getZoomInButton() {
        return zoomInButton;
    }

    public Button getZoomOutButton() {
        return zoomOutButton;
    }

    public Button getRemoveObjButton() {
        return removeObjButton;
    }

    public ColorPicker getColorPicker() {
        return colorPicker;
    }

    public Button getExplodeButton() {
        return explodeButton;
    }

    public CheckBox getRegexCheck() {
        return regexCheck;
    }

    public CheckBox getShowAICheck() {
        return showAICheck;
    }

    public TableView getAiHistoryTable() {
        return aiHistoryTable;
    }

    public VBox getAiButtonBox() {
        return aiButtonBox;
    }

    public Button getAskAIButton() {
        return askAIButton;
    }

    public Button getAiHistoryButton() {
        return aiHistoryButton;
    }

    public TextArea getAiPromptTextArea() {
        return aiPromptTextArea;
    }

    public CheckMenuItem getDarkModeMenuItem() {
        return darkModeMenuItem;
    }

    public MenuBar getTopMenuBar() {
        return topMenuBar;
    }

    public Button getInfoButton() {
        return infoButton;
    }

    public CheckBox getSelectionSynchCheck() {
        return selectionSynchCheck;
    }

    public CheckMenuItem getFullScreenCheck() {
        return fullScreenCheck;
    }

    public ToolBar getSearchToolBar() {
        return searchToolBar;
    }

    public MenuItem getMenuGuide() {
        return menuGuide;
    }

    public TreeView<ANode> getDumpTreeView() {return dumpTreeView;}

    public MenuItem getMenuSaveTree() {return menuSaveTree;}

    public TextField getFileIDTextfield() {return fileIDTextfield;}

    public Button getDrawEverythingButton() {return drawEverythingButton;}

    public Button getCutButt(){return cutButt;}

    public Label getBotLabelDrawCount() {return botLabelDrawCount;}

    public SplitMenuButton getCutSelectionButton() {return cutSelectionButton;}

    public MenuItem getCutAllMenuItem() {return cutAllMenuItem;}

    public MenuItem getCutSelectedMenuItem() {return cutSelectedMenuItem;}

    public Button getTiltClockwButton() {return tiltClockwButton;}

    public Button getTiltAntiClButton() {return tiltAntiClButton;}

    public CheckMenuItem getLoadBP3DV3checkMenu() {return loadBP3DV3checkMenu;}

    public MenuItem getLoadHierarchyMenu() {return loadHierarchyMenu;}

    public CheckMenuItem getEnableBP3DPartsCheckmenu() {return enableBP3DPartsCheckmenu;}
}
