package Project.window.Quiz;

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
     * @return if the quiz can go to the next question
     */
    boolean canNext();

    /**
     * @return if the quiz can go to the previous question
     */
    boolean canPrevious();

    /**
     * @return the next question
     */
    Question<?,?,S> next();

    /**
     * @return the next question
     */
    Question<?,?,S> previous();

    /**
     * @return the questions of this quiz
     */
    List<? extends Question<?,?,S>> getQuestions();  //not specifying the types of question because the quiz may have questions of different types.

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