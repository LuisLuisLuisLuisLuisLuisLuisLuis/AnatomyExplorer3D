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
    requires java.management;
    requires jdk.compiler;
    requires java.logging;
    requires jdk.xml.dom;
    requires jdk.jsobject;
    requires jdk.httpserver;
    requires com.dlsc.pdfviewfx;

    exports Project.AI.Format to com.fasterxml.jackson.databind;
    exports Project.window.ThreeDPaneHandling.Coloring to com.fasterxml.jackson.databind;

    opens Project.window to javafx.fxml;
    opens Project.window.TreeView.TreeAnalysis.MVPPattern to javafx.fxml;
    opens Project.window.Quiz to javafx.fxml;
    opens Project.window.Quiz.Generating to javafx.fxml;

    exports Project;
    opens Project.window.TreeView.TreeAnalysis to javafx.fxml;
    opens Project.model to javafx.fxml;
    opens Project.window.TreeView.TreeRestructuring to javafx.fxml;
    opens Project.window.TreeView.TreeViewEditing to javafx.fxml;
    opens Project.window.Quiz.Question to javafx.fxml;

}