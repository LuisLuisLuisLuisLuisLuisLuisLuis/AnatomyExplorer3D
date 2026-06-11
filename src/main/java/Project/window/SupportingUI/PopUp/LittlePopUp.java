package Project.window.SupportingUI.PopUp;


import Project.AnatomyExplorer;
import Project.SelectionModel.SelectionGroup;
import Project.SelectionModel.Tree.HasTreeView;
import Project.SelectionModel.Tree.TreeViewSelectionContainer;
import Project.model.ANode;
import Project.window.SupportingUI.TextSearch.SearchTree;
import Project.window.TreeView.TreeViewEditing.Command.UndoableANodeTreeViewEditor;
import Project.window.TreeView.TreeViewEditing.TreeViewSetup;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LittlePopUp {

    /**
     * Makes a little popup window with padding around the contents.
     * @param node: content of the window
     * @param title: Title of window
     * @param x: width
     * @param y: height
     */
    public static void showPaddedPopup(Node node, String title, int x, int y) {
        if (node == null) return;
        VBox root = new VBox(node);
        root.setPadding(new Insets(10));
        showPopup(root, title, x, y);
    }

    public static void showPopup(Node node, String title, int x, int y) {
        if (node == null) return;
        showPopup(new VBox(node), title, x, y);
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

    /**
     * Thought this would be needed but it may not be.
     */
    public static class ShowPopup {
        private final Scene scene;
        private final Stage stage;
        public Scene getScene() {return scene;}
        public Stage getStage() {return stage;}

        public ShowPopup(Parent root, String title, int x, int y) {
            this.scene = new Scene(root, x, y);
            this.stage = new Stage();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        }
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

    public static boolean showScrollableTextPopup(String title, String headertext, String text, boolean choice) {
        return showScrollableTextPopup(title, headertext, text, choice, "");
    }

    public static boolean showScrollableTextPopup(String title, String text, boolean choice) {
        return showScrollableTextPopup(title, null, text, choice, "");
    }

    public static boolean showScrollableTextPopup(String title, String text) {
        return showScrollableTextPopup(title, null, text, true, "");
    }
    /**
     * @param title Window title
     * @param headertext Header text
     * @param text Text to show
     * @param choice If yes -> OK and Cancel button. Else only OK button.
     * @return Button clicked == OK Button
     */
    public static boolean showScrollableTextPopup(String title, String headertext, String text, boolean choice, String okButtonText) {

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setResizable(true);
        dialog.setTitle(title);
        dialog.setHeaderText(headertext);

        ButtonType okType =
                new ButtonType(okButtonText.isBlank() ? "OK" : okButtonText, ButtonBar.ButtonData.OK_DONE);

        if (choice) dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);
        else dialog.getDialogPane().getButtonTypes().add(okType);

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

        Label headerLineEdge = new Label("- First line is header and will be skipped");
        Label headerLineFiles = new Label("- First line is header and will be skipped");
        Label edgeForm = new Label("- Format: ");
        bullets.add(headerLineEdge);
        bullets.add(edgeForm);

        TextFlow edgeFlow = new TextFlow();
        for (String txt : new String[]{"parent id", "parent name", "child id (optional)", "child name (optional)"}) {
            edgeFlow.getChildren().add(new Text(txt));
            Text tabTxt=new Text("\\t");
            tabTxt.setStyle("-fx-font-weight: bold");
            tabTxt.getStyleClass().add("text-normal");
            edgeFlow.getChildren().add(tabTxt);
        } edgeFlow.getChildren().removeLast();
        HBox edgeFormatHBox = new HBox(headerLineEdge, edgeForm, edgeFlow);

        Label edgeText2 = new Label(
                """
                        - No duplicate nodes. Node identity is inferred from node ID. Node names will be displayed in the tree.
                        - One parent per child, no cycles.
                        """);
        bullets.add(edgeText2);

        Label fileText = new Label("\nList of file associations (optional) with the following constraints:");
        paragraphs.add(fileText);

        Label fileIDtext = new Label("- Format: ");
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
        listVBox.getChildren().addAll(edgeText, headerLineEdge, edgeFormatHBox, edgeText2, fileText, headerLineFiles, fileIDformatbox, dirText, separator, askText);
        dialog.getDialogPane().setContent(listVBox);
        separator.autosize();
        ButtonType okType = new ButtonType("Continue", ButtonBar.ButtonData.NEXT_FORWARD);

        ButtonType cancelType = new ButtonType("Cancel" ,ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        dialog.setResultConverter(button -> button == okType);
        return dialog.showAndWait().orElse(false);
    }

    /**
     * Show a Popup with 3 buttons.
     * @return  OKButton ->  1, NOButton -> 0, Cancel   ->  -1
     */
    public static Integer showMsg(String title, String message, String okButtonText, String noButtonText, String cancelButtonText) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setContentText(message);
        dialog.setResizable(true);
        ButtonType okBType = new ButtonType(okButtonText, ButtonBar.ButtonData.OK_DONE);
        ButtonType noBType = new ButtonType(noButtonText, ButtonBar.ButtonData.NO);
        dialog.setResultConverter(
                button -> {
                    if (button == okBType) return 1;
                    if (button == noBType) return 0;
                    else return -1;
                }
        );
        dialog.getDialogPane().getButtonTypes().addAll(okBType, noBType, new ButtonType(cancelButtonText, ButtonBar.ButtonData.CANCEL_CLOSE));
        return dialog.showAndWait().orElse(-1);
    }

    public static boolean showMsg(String title, String message, String okButtonText, String noButtonText) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setContentText(message);
        dialog.setResizable(true);
        ButtonType okBType = new ButtonType(okButtonText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBType, new ButtonType(noButtonText, ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.setResultConverter(button -> button == okBType);
        return dialog.showAndWait().orElse(false);
    }

    /**
     * Display a message in a PopUp with only one Button.
     * @param title Popup window title
     * @param message Message
     * @param okButtonText Text of button
     */
    public static void showMsg(String title, String message, String okButtonText) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setContentText(message);
        dialog.setResizable(true);
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

    /**
     * Show some checkboxes and return the texts of the ones selected upon close or null if user cancels.
     * @param options Each option will be the text of a CheckBox.
     * @param title Title of window
     * @param message Header message
     * @return List of Texts of selected checkboxes or null if cancelled.
     */
    public static List<String> scrollableListPopup(List<String> options, String title, String message) {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(message);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        List<CheckBox> checkBoxes = new ArrayList<>(options.size());
        for (String option : options) checkBoxes.add(new CheckBox(option));

        VBox content = new VBox(10);
        content.getChildren().addAll(checkBoxes);

        dialog.setResultConverter(button -> {
            if (button == okButton) {
                List<String> selected = new ArrayList<>();
                for (CheckBox checkBox : checkBoxes) if (checkBox.isSelected()) selected.add(checkBox.getText());
                return selected;
            }
            return null; // cancel
        });
        dialog.getDialogPane().setContent(content);

        Optional<List<String>> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    
    public static TreeItem<ANode> selectTreeItemDialog(TreeItem<ANode> root) {

        // Dialog
        Dialog<TreeItem<ANode>> dialog = new Dialog<>();
        dialog.setResizable(true);
        dialog.setTitle("Tree Selection");
        BorderPane content = new BorderPane();

        // seutp TreeView
        TreeView<ANode> treeView = new TreeView<>(root);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        TreeViewSetup treeViewSetup = new TreeViewSetup(new UndoableANodeTreeViewEditor());
        treeViewSetup.setCellFactory(treeView, new UndoableANodeTreeViewEditor(), root.getValue(), false);
        treeView.setShowRoot(true);
        treeView.getSelectionModel().select(treeView.getRoot()); // so something is always selected when the user presses OK
                                                                 // -> null is returned iff the user pressed cancel

        // text search
        SearchTree searchTree = new SearchTree(treeView);
        TextField treeSearchField = new TextField();
        CheckBox regexCheck = new CheckBox();
        treeSearchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !treeSearchField.getText().isEmpty()) {
                searchTree.find(treeSearchField.getText(), false, regexCheck.isSelected());
            }
        });
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {if (event.isShortcutDown() && event.getCode() == KeyCode.F) treeSearchField.requestFocus();});

        Button treeSearchNextButton = new Button(">");
        Button treeSearchPrevButton = new Button("<");

        treeSearchNextButton.setOnAction(e -> searchTree.next());
        treeSearchNextButton.disableProperty().bind(searchTree.getCanNextObservable().not());
        treeSearchPrevButton.setOnAction(e -> searchTree.previous());
        treeSearchPrevButton.disableProperty().bind(searchTree.getHasPreviousObservable());

        ToolBar topBar = new ToolBar(treeSearchField, regexCheck, treeSearchPrevButton, treeSearchNextButton);

        // dont create Button buttons because they dont work with dialog result converter
        ButtonType acceptType = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(acceptType, cancelType);

        Button acceptBtn = (Button) dialog.getDialogPane().lookupButton(acceptType);

        acceptBtn.setDefaultButton(false);  // IMPORTANT else accept button will fire on any and all Enter pressed events (such as when pressing enter in the search field)

        content.setTop(topBar);
        content.setCenter(treeView);

        dialog.getDialogPane().setContent(content);

        // Result conversion
        dialog.setResultConverter(button -> {
            if (button == acceptType) {
                return treeView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // Show dialog
        Optional<TreeItem<ANode>> result = dialog.showAndWait();
        return result.orElse(null);
    }


    public record SelectTreeItemResult(TreeItem<ANode> result, String treeName) {

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SelectTreeItemResult that = (SelectTreeItemResult) o;
            return Objects.equals(treeName, that.treeName) && Objects.equals(result, that.result);
        }

        @Override
        public int hashCode() {
            return Objects.hash(result, treeName);
        }

        @Override
        public @NotNull String toString() {
            return result.getValue().toString() + " (" + treeName + ")";
        }
    }

    /**
     * Creates a Tab with a TreeView for each root item and allows the user to choose one TreeItem.
     * @param roots a list of TreeItems. Each one of them will be used as a root of a separate TreeView.
     * @param treeNames names of the trees. will be used to label to which TreeView (and thus to which root) the picked TreeItem belongs.
     * @param title title to be given to the window. set as null or empty to display default title.
     * @param message a message to be displayed on top of the window. leave null or empty to omit.
     * @return the picked TreeItem and the name of the TreeView it was picked from.
     */
    public static SelectTreeItemResult selectTreeItemDialog(List<TreeItem<ANode>> roots, List<String> treeNames, String title, String message) {

        if (roots.size() != treeNames.size() || roots.isEmpty()) return null;

        Dialog<SelectTreeItemResult> dialog = new Dialog<>();
        dialog.setResizable(true);
        dialog.setTitle((title == null || title.isBlank() ? "Tree Selection" : title));
        if (message != null && !message.isBlank()) dialog.setHeaderText(message);

        SelectionGroup<ANode> selectionGroup = new SelectionGroup<>("treeSelectionGroup");

        BorderPane content = new BorderPane();

        // --- TabPane with multiple TreeViews ---
        TabPane tabPane = new TabPane();

        // global selection holder
//        ObjectProperty<TreeItem<ANode>> selectedItem = new SimpleObjectProperty<>();
        ObjectProperty<SelectTreeItemResult> selectedItem = new SimpleObjectProperty<>();

        // search-related (will point to currently active tree)
        ObjectProperty<SearchTree> activeSearchTree = new SimpleObjectProperty<>();

        for (int i = 0; i < roots.size(); i++) {

            TreeItem<ANode> root = roots.get(i);

            TreeView<ANode> treeView = new TreeView<>(root);
            treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            treeView.setId(treeNames.get(i));

            HasTreeView<ANode> hasTreeView = new HasTreeView<>(treeView);
            TreeViewSelectionContainer treeViewSelectionContainer = new TreeViewSelectionContainer(hasTreeView);
            selectionGroup.addSelectionContainer(treeViewSelectionContainer);

            TreeViewSetup treeViewSetup = new TreeViewSetup(new UndoableANodeTreeViewEditor());
            treeViewSetup.setCellFactory(treeView, new UndoableANodeTreeViewEditor(), root.getValue(), false);

            treeView.setShowRoot(true);
            treeView.getSelectionModel().select(treeView.getRoot());

            // update global selection when this tree changes
            treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectedItem.set(new SelectTreeItemResult(newVal, treeView.getId()));
                }
            });

            // create SearchTree for this TreeView
            SearchTree searchTree = new SearchTree(treeView);

            Tab tab = new Tab(treeNames.get(i), treeView);
            tab.setClosable(false);
            tab.setUserData(searchTree); // store for later
            tabPane.getTabs().add(tab);

            // initialize selection & search with first tab
            if (i == 0) {
                selectedItem.set(new SelectTreeItemResult(treeView.getRoot(), treeView.getId()));
                activeSearchTree.set(searchTree);
            }
        }

        // --- Switch active search when tab changes ---
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                activeSearchTree.set((SearchTree) newTab.getUserData());
            }
        });

        // --- Search UI ---
        TextField treeSearchField = new TextField();
        CheckBox regexCheck = new CheckBox();

        treeSearchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !treeSearchField.getText().isEmpty()) {
                SearchTree st = activeSearchTree.get();
                if (st != null) {
                    st.find(treeSearchField.getText(), false, regexCheck.isSelected());
                }
            }
        });

        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
                treeSearchField.requestFocus();
            }
        });

        Button nextBtn = new Button(">");
        Button prevBtn = new Button("<");

        nextBtn.setOnAction(e -> {
            if (activeSearchTree.get() != null) activeSearchTree.get().next();
        });

        prevBtn.setOnAction(e -> {
            if (activeSearchTree.get() != null) activeSearchTree.get().previous();
        });

        // NOTE: bindings per active tree are tricky → simplest is rebind on tab change
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                SearchTree st = (SearchTree) newTab.getUserData();

                nextBtn.disableProperty().unbind();
                prevBtn.disableProperty().unbind();

                nextBtn.disableProperty().bind(st.getCanNextObservable().not());
                prevBtn.disableProperty().bind(st.getHasPreviousObservable());
            }
        });

        // initialize bindings for first tab
        if (!tabPane.getTabs().isEmpty()) {
            SearchTree st = (SearchTree) tabPane.getTabs().get(0).getUserData();
            nextBtn.disableProperty().bind(st.getCanNextObservable().not());
            prevBtn.disableProperty().bind(st.getHasPreviousObservable());
        }

        ToolBar topBar = new ToolBar(treeSearchField, regexCheck, prevBtn, nextBtn);

        // --- Buttons ---
        ButtonType acceptType = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(acceptType, cancelType);

        Button acceptBtn = (Button) dialog.getDialogPane().lookupButton(acceptType);
        acceptBtn.setDefaultButton(false);

        content.setTop(topBar);
        content.setCenter(tabPane);

        dialog.getDialogPane().setContent(content);

        // --- Result ---
        dialog.setResultConverter(button -> {
            if (button == acceptType) {
                return selectedItem.get();
            }
            return null;
        });

        Optional<SelectTreeItemResult> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public static List<String> test(List<String> options, String title, String message) {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(message);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        List<CheckBox> checkBoxes = new ArrayList<>(options.size());
        for (String option : options) checkBoxes.add(new CheckBox(option));

        VBox content = new VBox(10);
        content.getChildren().addAll(checkBoxes);
        Button test = new Button("click me");
        for (CheckBox checkBox : checkBoxes) checkBox.setVisible(false);
        test.setOnAction(e -> {for (CheckBox checkBox : checkBoxes) checkBox.setVisible(true);});
        content.getChildren().add(test);

        dialog.setResultConverter(button -> {
            if (button == okButton) {
                List<String> selected = new ArrayList<>();
                for (CheckBox checkBox : checkBoxes) if (checkBox.isSelected()) selected.add(checkBox.getText());
                return selected;
            }
            return null; // cancel
        });
        dialog.getDialogPane().setContent(content);

        Optional<List<String>> result = dialog.showAndWait();
        return result.orElse(null);
    }


}
    

