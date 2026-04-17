package Project.window.Quiz;


import Project.command.Command;

/**
 * @param <Q> type of the question
 * @param <S> the type of the score used to score the answer. A smaller score is interpreted as worse than a higher score.
 * @param <T> the type of the answer
 */
public interface Question<Q, T , S extends Comparable<S>> {

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
     * @return the default score for now answer
     */
    S defaultScore();

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
