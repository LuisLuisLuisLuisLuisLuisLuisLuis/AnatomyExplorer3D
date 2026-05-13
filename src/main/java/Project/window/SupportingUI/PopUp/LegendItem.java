package Project.window.SupportingUI.PopUp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

public class LegendItem extends HBox {
    public static final Pos DEFAULT_ALIGNMENT_POS = Pos.CENTER_LEFT;
    public static final Insets DEFAULT_PADDING = new Insets(10);
    public static final int DEFAULT_SPACING = 15;

    public LegendItem(Paint color, String text) {
        Circle dot = new Circle(6);
        dot.setFill(color);

        Label label = new Label(text);

        setSpacing(5);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(dot, label);
    }
    public LegendItem(String cssColor, String text) {
        this(Color.valueOf(cssColor), text);
    }
}