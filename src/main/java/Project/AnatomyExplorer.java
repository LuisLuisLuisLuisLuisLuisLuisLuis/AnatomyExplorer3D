package Project;

import Project.model.Model;
import Project.window.WindowView;
import Project.window.WindowPresenter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;

public class AnatomyExplorer extends Application {

    public void start(Stage primaryStage) throws Exception {
        WindowView view = new WindowView();

        InputStream partOf_parts_list_file = getClass().getResourceAsStream("/Project/anatomy/partof_parts_list_e.txt");    //parts_list is unused because its useless information for me.
        InputStream partOf_element_parts = getClass().getResourceAsStream("/Project/anatomy/partof_element_parts.txt");
        InputStream partOf_inclusion_relation_list = getClass().getResourceAsStream("/Project/anatomy/partof_inclusion_relation_list.txt");
        InputStream isA_parts_list_file = getClass().getResourceAsStream("/Project/anatomy/isa_parts_list_e.txt");
        InputStream isA_element_parts = getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_parts_v4_format.txt");
        InputStream isA_inclusion_relation_list = getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_composite_isa.txt");
        InputStream dumptree_relations = getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_conventional_partof.txt");
        InputStream dumptree_files = getClass().getResourceAsStream("/Project/anatomy/bp3d_v3_parts_v4_format.txt");

        WindowPresenter windowPresenter = new WindowPresenter(view.getController(), new Model(
                partOf_element_parts,
                partOf_inclusion_relation_list,
                isA_element_parts,
                isA_inclusion_relation_list,
                dumptree_relations,
                dumptree_files
        ),
                "/Project/anatomy/partof_BP3D_4.0_obj_99/",
                "/Project/anatomy/isa_BP3D_4.0_obj_99/"
        );

        primaryStage.setTitle("Tree Editor");
        Scene primaryScene = new javafx.scene.Scene(view.getRoot(), 800, 600);
        windowPresenter.setKeyControls(primaryScene);
        primaryStage.setScene(primaryScene);

        // enable dark mode
        URL darkModeCSS = WindowPresenter.class.getResource("/Project/Styles/modena_dark.css");

        windowPresenter.getDarkModeSelectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) primaryScene.getStylesheets().add(darkModeCSS.toExternalForm());
            else primaryScene.getStylesheets().remove(darkModeCSS.toExternalForm());
        });
        // enable full screen
        windowPresenter.getFullScreenCheckProperty().addListener(((observable, oldValue, newValue) -> {
            primaryStage.setFullScreen(newValue);
        }));

        primaryStage.show();

    }
}

