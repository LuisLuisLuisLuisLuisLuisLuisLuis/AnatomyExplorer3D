package Project.window.Quiz.Question;

import Project.window.Quiz.Difficulty;

import java.util.logging.Logger;

public abstract class SimpleQuestion<Q,T,S extends Comparable<S>> implements Question<Q,T,S> {

    private static final Logger logger = Logger.getLogger(SimpleQuestion.class.getName());

    private final Difficulty difficulty;
    private final S minScore;
    private final S defaultScore;
    private final S maxScore;
    private S currentScore;
    private T submittedAnswer;
    protected boolean isAnswered;
    protected final T correctAnswer;

    private String name;
    public void setName(String name) {this.name = name;}

    private String instructions;
    public void setInstructions(String instructions) {this.instructions = instructions;}

    /**
     * A simple question model. Implement ask() and evaluate() to use.
     * @param difficulty difficulty of the question
     * @param minScore minimum score. Also used as default score for no answer.
     * @param maxScore max achievable score on this question
     * @param correctAnswer correct answer
     */
    public SimpleQuestion(
            Difficulty difficulty,
            S minScore,
            S maxScore,
            T correctAnswer
            ) {
        this(difficulty, minScore, minScore, maxScore, correctAnswer);
    }

    /**
     * A simple question model. Implement ask() and evaluate() to use.
     * @param difficulty difficulty of the question
     * @param minScore minimum score. Can be lower than default score to implement punishment of wrong answers or higher to acknowledge the act of trying even when the answer is wrong.
     * @param defaultScore default score for no answer
     * @param maxScore max achievable score on this question
     * @param correctAnswer correct answer
     */
    public SimpleQuestion(
            Difficulty difficulty,
            S minScore,
            S defaultScore,
            S maxScore,
            T correctAnswer
    ) {
        this.difficulty = difficulty;
        this.minScore = minScore;
        this.defaultScore = defaultScore;
        this.maxScore = maxScore;
        this.correctAnswer = correctAnswer;
        this.currentScore = defaultScore;
    }

    @Override
    public void submit(T answer) {
        currentScore = evaluate(answer);
        submittedAnswer = answer;
        isAnswered = true;
    }

    @Override
    public S maxScore() {
        return maxScore;
    }

    @Override
    public S defaultScore() {
        return defaultScore;
    }

    @Override
    public S minScore() {
        return minScore;
    }


    @Override
    public boolean isAnswered() {
        return isAnswered;
    }

    @Override
    public S getScore() {
        return currentScore;
    }

    @Override
    public T getSubmittedAnswer() {
        return submittedAnswer;
    }

    @Override
    public Difficulty getDifficulty() {
        return difficulty;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getInstructions() {
        return instructions;
    }
}
