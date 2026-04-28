package Project.window.Quiz;

import Project.Util.Lorem;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import org.w3c.dom.Text;

import javax.swing.*;
import java.util.LinkedList;

public class QuestionFormat {

    public static final int DEFAULT_CHECK_SPACING = 5;
    public static final int DEFAULT_SPACING = 10;
    public static final Insets DEFAULT_PADDING = new Insets(10);

    public static <S> BorderPane stacked(String name, String instructions, Difficulty difficulty, S maxScore, S minScore, Node ui) {
        BorderPane result = new BorderPane();
        HBox top = new HBox();

        top.setPadding(DEFAULT_PADDING);
        top.setSpacing(25);
        top.getChildren().add(new Label((name != null && !name.isBlank() ? name : "")));
        Region topSpacer1 = new Region();
        HBox.setHgrow(topSpacer1, Priority.ALWAYS);
        top.getChildren().add(topSpacer1);
        top.getChildren().add(new Label((difficulty != null ? "Difficulty: " + difficulty : "")));
        Region topSpacer2 = new Region();
        HBox.setHgrow(topSpacer2, Priority.ALWAYS);
        top.getChildren().add(topSpacer2);
        top.getChildren().add(new Label("Pts (min/max): " + minScore + "/" + maxScore));

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

        Label instructionArea = new Label(instructions);
        instructionArea.setWrapText(true);
        ScrollPane scrollPane = new ScrollPane(instructionArea);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollPane.setMinHeight(0);
        scrollPane.setFitToWidth(true);
        Region centerSpacer = new Region();
        VBox.setVgrow(centerSpacer, Priority.ALWAYS);
        centerBox.getChildren().addAll(uiContainer, scrollPane);

        result.setCenter(centerBox);

        return result;
    }
}
