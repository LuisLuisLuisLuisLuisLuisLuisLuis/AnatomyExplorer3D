package Project.window.Quiz;

public class MOCKQsimple<Q,T,S> extends SimpleQuestion<Q,T,S>{
    /**
     * A simple question model. Implement ask() and evaluate() to use.
     *
     * @param difficulty    difficulty of the question
     * @param minScore      minimum score
     * @param maxScore      max score
     * @param correctAnswer correct answer
     */
    public MOCKQsimple(Difficulty difficulty, S minScore, S maxScore, T correctAnswer) {
        super(difficulty, minScore, maxScore, correctAnswer);
    }

    @Override
    public Q ask() {
        return null;
    }

    @Override
    public S evaluate(T answer) {
        return null;
    }
}
