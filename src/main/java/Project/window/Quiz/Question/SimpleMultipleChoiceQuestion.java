package Project.window.Quiz.Question;

import Project.window.Quiz.Difficulty;

import java.util.HashSet;
import java.util.List;

public abstract class SimpleMultipleChoiceQuestion<Q,T,S extends Comparable<S>> extends SimpleQuestion<Q,List<T>,S> implements MultipleChoiceQuestion<Q,T,S> { // implements MultipleChoiceQuestion<Q,T,S>

    private final boolean exposesNumberOfCorrectChoices;

    /**
     * @return whether this instance exposes how many of the choices are correct
     */
    public boolean exposesNumberOfCorrectChoices() {return exposesNumberOfCorrectChoices;}

    /**
     * @return the number of correct choices or -1 if this instance does not expose the number of correct choices
     */
    public int numberOfCorrectChoices() {return exposesNumberOfCorrectChoices() ? correctAnswer.size() : -1;}

    protected final List<T> possibleAnswers;

    /**
     * @return the list of possible answers
     */
    public List<T> getChoices() {return possibleAnswers;}

    /**
     * A simple multiple choice question model with only one correct answer. Implement ask() and evaluate() to use.
     *
     * @param difficulty    difficulty of the question
     * @param minScore      minimum score
     * @param maxScore      max score
     * @param correctAnswer correct answer
     * @param possibleAnswers choices for this question. it is only possible to answer with one of the choices
     * @throws IllegalArgumentException if not all correct answer options are contained in the list of possible answers
     */
    public SimpleMultipleChoiceQuestion(Difficulty difficulty, S minScore, S maxScore, List<T> correctAnswer, List<T> possibleAnswers) {
        this(difficulty, minScore, maxScore, correctAnswer, possibleAnswers, false);
    }

    /**
     * A simple multiple choice question model with only one correct answer. Implement ask() and evaluate() to use.
     *
     * @param difficulty    difficulty of the question
     * @param minScore      minimum score
     * @param maxScore      max score
     * @param correctAnswer correct answer
     * @param possibleAnswers choices for this question. it is only possible to answer with one of the choices
     * @param exposesNumberOfCorrectChoices whether to expose how many of the choices are correct
     * @throws IllegalArgumentException if not all correct answer options are contained in the list of possible answers
     */
    public SimpleMultipleChoiceQuestion(Difficulty difficulty, S minScore, S maxScore, List<T> correctAnswer, List<T> possibleAnswers, boolean exposesNumberOfCorrectChoices) {
        super(difficulty, minScore, maxScore, correctAnswer);
        if (!new HashSet<>(possibleAnswers).containsAll(correctAnswer)) throw new IllegalArgumentException("All correct answer options must be contained in the list of possible answers. But\ncorrect answer=" + correctAnswer + "\npossible answers=" + possibleAnswers);
        this.possibleAnswers = possibleAnswers;
        this.exposesNumberOfCorrectChoices = exposesNumberOfCorrectChoices;
    }

    /**
     * Submit an answer. The answer must only contain options from the possible answers of this question.
     * @param answer answer
     * @throws IllegalArgumentException if one or more elements of the answer are not among the possible options.
     */
    @Override
    public void submit(List<T> answer) throws IllegalArgumentException{
        if (!new HashSet<>(possibleAnswers).containsAll(answer)) throw new IllegalArgumentException("Answer must only contain " + possibleAnswers + " but was " + answer);
        super.submit(answer);
    }

}
