package Project.window.TreeView.TreeRestructuring;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public class TreeRestructuringView {
    private final TreeRestructuringController controller;
    private final Parent root;

    public TreeRestructuringView() throws IOException {
        URL fxmlUrl = getClass().getResource("/Project/FXML/RestructureTreeUI.fxml");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        root = loader.load();
        controller = loader.getController();
    }

    public TreeRestructuringController getController() {return controller; }
    public Parent getRoot() { return root; }
}
