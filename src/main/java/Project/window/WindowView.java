package Project.window;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.IOException;
import java.net.URL;

public class WindowView {
    private final MainWindowController controller;
    private final Parent root;

    public WindowView() throws IOException {
        URL fxmlUrl = getClass().getResource("/Project/FXML/MainWindow.fxml");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        root = loader.load();
        controller = loader.getController();
    }

    public MainWindowController getController() {return controller; }
    public Parent getRoot() { return root; }
}
