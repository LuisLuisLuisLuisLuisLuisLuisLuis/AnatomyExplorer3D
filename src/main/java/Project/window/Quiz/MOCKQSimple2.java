package Project.window.Quiz;

public class MOCKQSimple2<Q,T,S> implements Question<Q,T,S>{
    @Override
    public Q ask() {
        return null;
    }

    @Override
    public void submit(T answer) {

    }

    @Override
    public T getSubmittedAnswer() {
        return null;
    }

    @Override
    public S evaluate(T answer) {
        return null;
    }

    @Override
    public S maxScore() {
        return null;
    }

    @Override
    public S minScore() {
        return null;
    }

    @Override
    public boolean isAnswered() {
        return false;
    }

    @Override
    public S getScore() {
        return null;
    }

    @Override
    public Difficulty getDifficulty() {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getInstructions() {
        return "";
    }
}
