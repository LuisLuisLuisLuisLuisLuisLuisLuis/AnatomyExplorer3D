package Project.window.Quiz;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;

public interface UIQuestion<T,S> extends Question<Node, T, S> {

    /**
     * Asks via a user interface.
     * @return the root of the UI.
     */
    @Override
    Node ask();

    /**
     * @return the answeredProperty
     */
    SimpleBooleanProperty getAnsweredProperty();
}