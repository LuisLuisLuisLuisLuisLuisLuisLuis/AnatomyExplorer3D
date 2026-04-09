package Project.window.Quiz;

import org.jetbrains.annotations.Contract;

import java.util.List;

/**
 * @param <S> the type of the score
 */
public interface Quiz<S> {

    /**
     * Start the quiz
     */
    void start();

    /**
     * Stop the quiz
     */
    void stop();

    /**
     * @return the questions of this quiz
     */
    List<Question> getQuestions();  //not specifying the types of question because the quiz may have questions of different types.

    /**
     * @return the current progress (between 0 and 1)
     */
    double getProgress();

    /**
     * @return the current score
     */
    S getScore();

    /**
     * @return the name of the quiz
     */
    String getName();

    /**
     * @return a description of the quiz if provided
     */
    String getDescription();

    /**
     * @return the best score achieved on this quiz
     */
    S getPB();
}