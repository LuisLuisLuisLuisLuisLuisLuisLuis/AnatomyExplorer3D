package Project.window.TreeView.TreeAnalysis.MVPPattern;

import Project.window.TreeView.TreeViewSetup;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class TreeAnalysisController {

    @FXML
    private Button searchButton;

    @FXML
    private Label botlabel;

    @FXML
    private Button removeFileIDButton;

    @FXML
    private Button addFileIDButton;

    @FXML
    private TextField addFileIDTextField;

    @FXML
    private TableView<TreeViewSetup.FileIDRow> fileIDTableView;

    public Button getAddFileIDButton() {
        return addFileIDButton;
    }

    public TextField getAddFileIDTextField() {
        return addFileIDTextField;
    }

    public TableView<TreeViewSetup.FileIDRow> getFileIDTableView() {
        return fileIDTableView;
    }

    public Button getRemoveFileIDButton() {
        return removeFileIDButton;
    }

    public Button getSearchButton() {return searchButton;}

    public Label getBotlabel() {return botlabel;}
}
