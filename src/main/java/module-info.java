module com.anatomyExplorer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires java.desktop;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires com.almasb.fxgl.all;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires java.net.http;
    requires org.jetbrains.annotations;

    exports Project.AI.Format to com.fasterxml.jackson.databind;
    exports Project.window.ThreeDPaneHandling.Coloring to com.fasterxml.jackson.databind;

    opens Project.window to javafx.fxml;
    opens Project.window.TreeView.TreeAnalysis.MVPPattern to javafx.fxml;

    exports Project;
    opens Project.window.TreeView to javafx.fxml;
    opens Project.window.TreeView.TreeAnalysis to javafx.fxml;
    opens Project.model to javafx.fxml;
    opens Project.window.TreeView.AITreeIntegration.MVPattern to javafx.fxml;
    opens Project.window.TreeView.TreeAnalysis.TreeViewEditing to javafx.fxml;

}