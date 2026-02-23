package Project.window.PopUp;


import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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



}


