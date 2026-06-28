package Project.window.Quiz;

import Project.window.Quiz.Question.UIQuestion;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class UIQuizInt extends UIQuiz<Integer>{

    public UIQuizInt(List<UIQuestion<?, Integer>> uiQuestions, boolean showScore, boolean showCorrect, int allowedTime, TimeUnit timeUnit) {
        super(uiQuestions, showScore, showCorrect, allowedTime, timeUnit);
    }

    public UIQuizInt(List<UIQuestion<?, Integer>> uiQuestions, boolean showScore, boolean showCorrect) {
        this(uiQuestions, showScore, showCorrect, -1, TimeUnit.SECONDS);
    }

    @Override
    public Integer computeScore(List<Integer> scores) {
        int result = 0;
        for (int i : scores) result+=i;
        return result;
    }
}
