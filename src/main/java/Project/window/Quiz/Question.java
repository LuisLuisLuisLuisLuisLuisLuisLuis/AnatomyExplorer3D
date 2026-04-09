package Project.window.Quiz;

import org.jetbrains.annotations.NotNull;

/**
 * @param <S> the type of score used to evaluate the answer
 * @param <T> the type of the answer
 */
public interface Question<S, T> {
    /**
     * Asks the question.
     */
    void ask();

    /**
     * Submit the answer.
     * @param answer answer
     * @return the score for that answer.
     */
    S submit(@NotNull T answer);

    /**
     * Skips this question.
     * @return the score for giving no answer.
     */
    S skip();

    /**
     * @return true if this question has been skipped.
     */
    boolean isSkipped();

    /**
     * @return false as long as the question has not been answered and true as soon as an answer has been submitted.
     */
    boolean isAnswered();

    /**
     * @return the score for the given answer. Returns null as long as no answer is given.
     */
    S getScore();

    /**
     * @return the answer submitted by the user. Returns null as long as no answer is given.
     */
    T getSubmittedAnswer();

    /**
     * @return the correct answer.
     */
    T getCorrectAnswer();
}
