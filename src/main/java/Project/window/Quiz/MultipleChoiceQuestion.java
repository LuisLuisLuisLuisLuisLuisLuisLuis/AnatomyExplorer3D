package Project.window.Quiz;

import java.util.List;

public interface MultipleChoiceQuestion<Q,T,S> extends Question<Q,List<T>,S>{
    List<T> getChoices();
    int numberOfCorrectChoices();
    boolean exposesNumberOfCorrectChoices();
}

