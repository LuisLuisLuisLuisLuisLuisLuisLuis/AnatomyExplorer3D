package Project.window.Quiz;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.LinkedList;

public class QuestionFormat {

    public static final int DEFAULT_CHECK_SPACING = 5;
    public static final int DEFAULT_SPACING = 10;
    public static final Insets DEFAULT_PADDING = new Insets(10);

    public static <S> VBox stacked(String name, String instructions, Difficulty difficulty, S maxScore, S minScore, Node ui) {
        VBox result = new VBox();
        HBox top = new HBox();
        top.setPadding(DEFAULT_PADDING);
        top.setSpacing(25);
        if (name != null && !name.isBlank()) top.getChildren().add(new Label(name));
        if (difficulty != null) top.getChildren().add(new Label("Difficulty: " + difficulty));
        top.getChildren().add(new Label("Pts (min/max): " + minScore + "/" + maxScore));

        result.setSpacing(DEFAULT_SPACING);
        result.setPadding(DEFAULT_PADDING);
        result.getChildren().add(top);
        if (instructions != null && !instructions.isBlank()) result.getChildren().add(new Label(instructions));
        result.getChildren().add(ui);
        return result;
    }
}
