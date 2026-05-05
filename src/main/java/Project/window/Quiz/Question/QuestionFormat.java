package Project.window.Quiz.Question;

import Project.window.Quiz.Difficulty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

public class QuestionFormat {

    public static final int DEFAULT_CHECK_SPACING = 5;
    public static final int DEFAULT_SPACING = 10;
    public static final Insets DEFAULT_PADDING = new Insets(10);

    public static <S> BorderPane stacked(String name, String instructions, Difficulty difficulty, S maxScore, S minScore, Node ui) {
        BorderPane result = new BorderPane();
        HBox top = new HBox();

        top.setPadding(DEFAULT_PADDING);
        top.setSpacing(25);
        Label nameLabel = new Label((name != null && !name.isBlank() ? name : ""));
        nameLabel.setMinWidth(Region.USE_PREF_SIZE);

        top.getChildren().add(nameLabel);
        Region topSpacer1 = new Region();
        HBox.setHgrow(topSpacer1, Priority.ALWAYS);
        top.getChildren().add(topSpacer1);
        Label diffLabel = new Label((difficulty != null ? "Difficulty: " + difficulty : ""));
        diffLabel.setMinWidth(Region.USE_PREF_SIZE);
        top.getChildren().add(diffLabel);
        Region topSpacer2 = new Region();
        HBox.setHgrow(topSpacer2, Priority.ALWAYS);
        top.getChildren().add(topSpacer2);
        Label ptsLabel = new Label("Pts (min/max): " + minScore + "/" + maxScore);
        ptsLabel.setMinWidth(Region.USE_PREF_SIZE);
        top.getChildren().add(ptsLabel);

        result.setTop(top);
        VBox centerBox = new VBox();
        HBox uiContainer = new HBox();
        HBox.setHgrow(uiContainer, Priority.ALWAYS);
        Region uiSpacer1 = new Region();
        Region uiSpacer2 = new Region();
        HBox.setHgrow(uiSpacer1, Priority.ALWAYS);
        HBox.setHgrow(uiSpacer2, Priority.ALWAYS);
        uiContainer.getChildren().addAll(uiSpacer1, ui, uiSpacer2);

        BorderPane.setMargin(uiContainer, new Insets(10));

        Label instructionsLabel = new Label(instructions);
        instructionsLabel.setMinWidth(Region.USE_PREF_SIZE);
        ScrollPane scrollPane = new ScrollPane(instructionsLabel);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollPane.setPrefHeight(instructionsLabel.getPrefHeight());
        scrollPane.setFitToWidth(true);
        Region centerSpacer = new Region();
        VBox.setVgrow(centerSpacer, Priority.ALWAYS);
        centerBox.getChildren().addAll(uiContainer, scrollPane);

        result.setCenter(centerBox);

        return result;
    }
}
