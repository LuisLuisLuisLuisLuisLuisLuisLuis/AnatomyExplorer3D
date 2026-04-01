package Project;

import Project.Util.LoggingConfig;
import Project.window.WindowView;
import Project.window.WindowPresenter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;



public class AnatomyExplorer extends Application {

    public void start(Stage primaryStage) throws Exception {
        Locale.setDefault(Locale.ENGLISH);  // all default texts are in ENG
        LoggingConfig.init();

        WindowView view = new WindowView();

        InputStream partOf_parts_list_file = getClass().getResourceAsStream("/Project/anatomy/partof_parts_list_e.txt");    //parts_list is unused because its useless information for me.
        InputStream isA_parts_list_file = getClass().getResourceAsStream("/Project/anatomy/isa_parts_list_e.txt");


        primaryStage.setTitle("Anatomy Viewer");
        Scene primaryScene = new javafx.scene.Scene(view.getRoot(), 900, 600);

        primaryStage.setScene(primaryScene);

        primaryStage.show();

        WindowPresenter windowPresenter = new WindowPresenter(view.getController());
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            windowPresenter.requestExit();
        });
        windowPresenter.setKeyControls(primaryScene);


        primaryScene.getStylesheets().add(getClass().getResource("/Project/Styles/fonts.css").toExternalForm());

        // enable dark mode
        URL darkModeCSS = WindowPresenter.class.getResource("/Project/Styles/modena_dark.css");
        //TODO: toExternalForm may be bad for shipping.
        windowPresenter.getDarkModeSelectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) primaryScene.getStylesheets().add(darkModeCSS.toExternalForm());
            else primaryScene.getStylesheets().remove(darkModeCSS.toExternalForm());
        });
        // enable full screen
        windowPresenter.getFullScreenCheckProperty().addListener(((observable, oldValue, newValue) -> {
            primaryStage.setFullScreen(newValue);
        }));



    }

    public static String FONTS_CSS =
            AnatomyExplorer.class.getResource("/Project/Styles/fonts.css")
                    .toExternalForm();
}

