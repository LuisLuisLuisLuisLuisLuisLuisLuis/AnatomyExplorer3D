package Project.window.TreeView.TreeAnalysis.MVPPattern;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public class FileIDView {
    private final TreeAnalysisController controller;
    private final Parent root;

    public FileIDView() throws IOException {
        URL fxmlUrl = getClass().getResource("/Project/FXML/FileIDInfo.fxml");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        root = loader.load();
        controller = loader.getController();
    }

    public TreeAnalysisController getController() {return controller; }
    public Parent getRoot() { return root; }
}
