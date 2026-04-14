package Project.window.Quiz;


/**
 * @param <Q> type of the question
 * @param <S> the type of score used to evaluate the answer
 * @param <T> the type of the answer
 */
public interface Question<Q, T, S> {

    /**
     * @return the question
     */
    Q ask();

    /**
     * Submit the answer.
     * @param answer answer
     */
    void submit(T answer);

    /**
     * @return the answer submitted by the user.
     */
    T getSubmittedAnswer();

    /**
     * Evaluate the answer and return a score.
     * @param answer answer to the question
     * @return the score for that answer.
     */
    S evaluate(T answer);

    /**
     * @return the best possible score you can get
     */
    S maxScore();

    /**
     * @return the worst possible score you can get
     */
    S minScore();

    /**
     * @return false as long as the question has not been answered and true as soon as an answer has been submitted.
     */
    boolean isAnswered();

    /**
     * @return the score for the given answer.
     */
    S getScore();

    /**
     * @return the difficulty of this question
     */
    Difficulty getDifficulty();

    /**
     * @return the name of this question
     */
    String getName();

    /**
     * @return instructions for this question
     */
    String getInstructions();

}
