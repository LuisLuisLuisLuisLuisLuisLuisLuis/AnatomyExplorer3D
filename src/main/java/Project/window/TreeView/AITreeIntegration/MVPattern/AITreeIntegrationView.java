package Project.window.TreeView.AITreeIntegration.MVPattern;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public class AITreeIntegrationView {
    private final AITreeIntegrationController controller;
    private final Parent root;

    public AITreeIntegrationView() throws IOException {
        URL fxmlUrl = getClass().getResource("/Project/FXML/AITreeIntegration.fxml");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        root = loader.load();
        controller = loader.getController();
    }

    public AITreeIntegrationController getController() {return controller; }
    public Parent getRoot() { return root; }
}
