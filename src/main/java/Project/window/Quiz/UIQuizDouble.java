package Project.window.Quiz;

import Project.window.Quiz.Question.UIQuestion;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class UIQuizDouble extends UIQuiz<Double>{

    public UIQuizDouble(List<UIQuestion<?, Double>> uiQuestions, boolean showScore, boolean showCorrect, int allowedTime, TimeUnit timeUnit) {
        super(uiQuestions, showScore, showCorrect, allowedTime, timeUnit);
    }

    public UIQuizDouble(List<UIQuestion<?, Double>> uiQuestions, boolean showScore, boolean showCorrect) {
        super(uiQuestions, showScore, showCorrect);
    }

    @Override
    public Double computeScore(List<Double> scores) {
        double result = 0;
        for (double d : scores) result+=d;
        return result;
    }
}
