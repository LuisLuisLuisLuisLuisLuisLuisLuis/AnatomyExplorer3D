package Project.window.Quiz.Question;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

public interface UIQuestion<T,S extends Comparable<S>> extends Question<Node, T, S> {

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

    /**
     * @return the score property
     */
    SimpleObjectProperty<S> getScoreProperty();

    /**
     * @return the submittedAnswer property
     */
    SimpleObjectProperty<T> submittedAnswerProperty();

    /**
     * @param flag whether to show the correct answer in the GUI upon submission of a wrong answer
     */
    void showCorrectAnswerOnWrongSubmit(boolean flag);

}