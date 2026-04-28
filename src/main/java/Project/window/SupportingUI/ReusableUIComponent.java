package Project.window.SupportingUI;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.function.Function;

public class ReusableUIComponent {

    /**
     * Creates a Tag like component displaying item.toString() and a button.
     * @param item item that the Tag holds.
     * @param onClick function that will be called when the button is pressed.
     * @return the tag. the item will be set as user data.
     * @param <T> type of item
     * @param <R> return type of function
     * @param symbol symbol of the button
     */
    public static <T, R> HBox createTag(T item, Function<T, R> onClick, String symbol) {
        Label label = new Label(item.toString());

        Button button = new Button(symbol);
        button.setOnAction(e -> onClick.apply(item));

        HBox box = new HBox(label, button);
        box.setUserData(item);
        box.setSpacing(5);
        box.setStyle("-fx-background-color: lightgray; -fx-padding: 5; -fx-background-radius: 5;");

        return box;
    }
}
