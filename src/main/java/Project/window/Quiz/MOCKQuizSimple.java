package Project.window.Quiz;

import java.util.List;

public class MOCKQuizSimple<S> implements Quiz<S>{

    private final List<MOCKQSimple2<?,?,S>> questions;

    public MOCKQuizSimple (List<MOCKQSimple2<?,?,S>> questions) {
        this.questions = questions;
    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean canNext() {
        return false;
    }

    @Override
    public boolean canPrevious() {
        return false;
    }

    @Override
    public Question<?, ?, S> next() {
        return null;
    }

    @Override
    public Question<?, ?, S> previous() {
        return null;
    }

    @Override
    public List<? extends Question<?, ?, S>> getQuestions() {
        return questions;
    }

    @Override
    public double getProgress() {
        return 0;
    }

    @Override
    public S getScore() {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public S getPB() {
        return null;
    }
}
