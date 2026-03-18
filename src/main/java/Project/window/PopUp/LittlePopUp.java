package Project.window.PopUp;


import Project.AnatomyExplorer;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.function.Function;

public class LittlePopUp {

    /**
     * Makes a little popup window
     * @param node: content of the window
     * @param title: Title of window
     * @param x: width
     * @param y: height
     */
    public static void showChartPopup(Node node, String title, int x, int y) {
        if (node == null) return;
        VBox root = new VBox(node);
        root.setPadding(new Insets(10));
        showPopup(root, title, x, y);
    }

    /**
     * Creates and shows a new Stage with the given root, title and dimensions.
     * @return The scene.
     */
    public static Scene showPopup(Parent root, String title, int x, int y) {
        Scene scene = new Scene(root, x, y);
        Stage popupStage = new Stage();
        popupStage.setTitle(title);
        popupStage.setScene(scene);
        popupStage.show();
        return scene;   // so that the invoker may set key_pressed event listener
    }

    public static Integer askForGreater0Int() {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Choose n");

        ButtonType okButtonType =
                new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(okButtonType, new ButtonType("Cancel" ,ButtonBar.ButtonData.CANCEL_CLOSE));

        TextField numberField = new TextField();
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        VBox content = new VBox(10, numberField, errorLabel);
        dialog.getDialogPane().setContent(content);

        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);

        okButton.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                int i = Integer.parseInt(numberField.getText());
                if (i < 1) {
                    errorLabel.setText("Number must be ≥ 1");
                    e.consume();
                    return;
                }
                errorLabel.setText("");
            } catch (IllegalArgumentException ex) {
                errorLabel.setText("Please enter a valid number");
                e.consume(); // prevents dialog from closing
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt == okButtonType) {
                return Integer.parseInt(numberField.getText());
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    public static boolean showScrollableTextPopup(String title, String text) {

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);

        ButtonType okType =
                new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes()
                .addAll(okType, ButtonType.CANCEL);

        // Scrollable text area (best for long lists)
        TextArea textArea = new TextArea(text);
        textArea.setEditable(false);
        textArea.setWrapText(false);

        textArea.setPrefWidth(600);
        textArea.setPrefHeight(400);

        dialog.getDialogPane().setContent(textArea);

        dialog.setResultConverter(bt -> bt == okType);

        return dialog.showAndWait().orElse(false);
    }

    public static boolean showLoadHierInfo() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(AnatomyExplorer.FONTS_CSS);    //set the css

        dialog.setTitle("Load Hierarchy");
        dialog.setHeaderText("To load a hierarchy, the following are required:");

        VBox listVBox = new VBox();
        LinkedList<Label> paragraphs = new LinkedList<>();  //hold Labels and format them in bulk
        LinkedList<Label> bullets = new LinkedList<>();

        Label edgeText = new Label("Edge list with the following constraints:");
        paragraphs.add(edgeText);

        Label edgeForm = new Label("- Format: ");
        bullets.add(edgeForm);

        TextFlow edgeFlow = new TextFlow();
        for (String txt : new String[]{"parent id", "parent name", "child id (optional)", "child name (optional)"}) {
            edgeFlow.getChildren().add(new Text(txt));
            Text tabTxt=new Text("\\t");
            tabTxt.setStyle("-fx-font-weight: bold");
            tabTxt.getStyleClass().add("text-normal");
            edgeFlow.getChildren().add(tabTxt);
        } edgeFlow.getChildren().removeLast();
        HBox edgeFormatHBox = new HBox(edgeForm, edgeFlow);

        Label edgeText2 = new Label(
                """
                        - No duplicate nodes. Node identity is inferred from node ID. Node names will be displayed in the tree.
                        - One parent per child, no cycles.
                        """);
        bullets.add(edgeText2);

        Label fileText = new Label("\nList of file associations (optional) with the following constraints:");
        paragraphs.add(fileText);

        Label fileIDtext = new Label("- format: ");
        bullets.add(fileIDtext);
        TextFlow fileIDTextFlow = new TextFlow();
        for (String txt : new String[]{"node id", "node name", "name of the .obj file"}) {
            Text text = new Text(txt);
            text.getStyleClass().add("text-normal");
            fileIDTextFlow.getChildren().add(text);
            Text tabTxt=new Text("\\t");
            tabTxt.setStyle("-fx-font-weight: bold");
            tabTxt.getStyleClass().add("text-normal");
            fileIDTextFlow.getChildren().add(tabTxt);
        } fileIDTextFlow.getChildren().removeLast();
        HBox fileIDformatbox = new HBox(fileIDtext, fileIDTextFlow);

        Label dirText = new Label("\nDirectory containing the .obj files for the hierarchy (optional).");
        dirText.getStyleClass().add("text-large");
        paragraphs.add(dirText);

        Label askText = new Label("Press 'Continue' to select the two files and the directory.");
        bullets.add(askText);

        for (Label label : paragraphs) {
            label.setTextAlignment(TextAlignment.LEFT);
            label.setWrapText(true);
            label.getStyleClass().add("text-large");
        }
        for (Label label : bullets) {
            label.setTextAlignment(TextAlignment.LEFT);
            label.setWrapText(true);
            label.getStyleClass().add("text-normal");
        }
        Separator separator = new Separator(Orientation.HORIZONTAL);
        separator.setPadding(new Insets(12,0,12,0));
        listVBox.getChildren().addAll(edgeText, edgeFormatHBox, edgeText2, fileText, fileIDformatbox, dirText, separator, askText);
        dialog.getDialogPane().setContent(listVBox);
        separator.autosize();
        ButtonType okType = new ButtonType("Continue", ButtonBar.ButtonData.NEXT_FORWARD);

        ButtonType cancelType = new ButtonType("Cancel" ,ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        dialog.setResultConverter(button -> button == okType);
        return dialog.showAndWait().orElse(false);
    }

    public static boolean showMsg(String title, String message, String okButtonText, String noButtonText) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(200); // desired max width
        dialog.getDialogPane().setContent(label);

        ButtonType okBType = new ButtonType(okButtonText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBType, new ButtonType(noButtonText, ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.setResultConverter(button -> button == okBType);
        return dialog.showAndWait().orElse(false);
    }

    public static void showMsg(String title, String message, String okButtonText) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setContentText(message);
        for (String s : message.split("\n")) {
            if (dialog.getWidth() < s.length() * 3) dialog.setWidth(s.length() * 3);
        }
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType(okButtonText, ButtonBar.ButtonData.OK_DONE));
        dialog.showAndWait();
    }


    public static String askForString(String title, String message) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setContentText(message);
        TextField textField = new TextField();
        Button okButton = new Button("Accept");
        okButton.setDisable(true);
        dialog.setResult("");
        dialog.setResultConverter(cancelbutton -> null);
        okButton.setOnAction(e -> {
            dialog.setResult(textField.getText());
            dialog.close();
        });
        dialog.getDialogPane().setContent(new HBox(5, textField, okButton));
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
        textField.textProperty().addListener((observable, oldValue, newValue) -> {okButton.setDisable(newValue.isBlank());});
        dialog.showAndWait();
        return dialog.getResult();
    }


}


