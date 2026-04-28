package Project.window.Quiz.Generating;

import Project.model.ANode;
import Project.model.Model;
import Project.window.PopUp.LittlePopUp;
import Project.window.Quiz.*;
import Project.window.SupportingUI.ReusableUIComponent;
import Project.window.TreeView.TreeAnalysis.TreeAnalysisUtils;
import Project.window.TreeView.TreeViewEditing.Command.UndoableANodeTreeViewEditor;
import Project.window.TreeView.TreeViewEditing.TreeViewSetup;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RandomUIQuizGenerator {

    private static final Logger logger = Logger.getLogger(RandomUIQuizGenerator.class.getName());

    private List<Runnable> runOnCreateList = new ArrayList<>();

    /**
     * @param runnable A Runnable to be run when this class has created a new Quiz.
     */
    public void addOnCreateRunnable(Runnable runnable) {this.runOnCreateList.add(runnable);}

    private void onCreate() {
        for (Runnable r : runOnCreateList) r.run();
    }

    private UIQuiz<Integer> integerUIQuiz = null;

    /**
     * @return the Integer UIQuiz created by the user, or null if none has been created (yet).
     */
    public UIQuiz<Integer> getIntegerUIQuiz() {return integerUIQuiz;}

    /**
     * @param fromModels Models from which the Quiz is to be generated.
     */
    public RandomUIQuizGenerator(List<Model> fromModels) {

        RandomUIQuizGeneratorView randomUIQuizGeneratorView = null;
        try {
             randomUIQuizGeneratorView = new RandomUIQuizGeneratorView();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "RandomUIQuizGeneratorView failed to start: " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()).replace(", ", "\n") + e.getCause());
            LittlePopUp.showMsg("Error", "Failed to start quiz generator", "OK");
            return;
        }
        QuizPropertyChooserController controller = randomUIQuizGeneratorView.getController();
        Scene scene = LittlePopUp.showPopup(randomUIQuizGeneratorView.getRoot(), "Choose quiz properties", 700,500);

        // set allowed values for textfields
        UnaryOperator<TextFormatter.Change> nquestionsFilter = change -> {
            String newText = change.getControlNewText();
            // Allow empty field (so user can delete)
            if (newText.isEmpty()) {return change;}
            try {
                // only 1-100 questions allowed
                int value = Integer.parseInt(newText);

                if (value > 0 && value < 101) return change;

            } catch (NumberFormatException ignored) {} // only numbers allowed

            return null; // reject change
        };
        UnaryOperator<TextFormatter.Change> timeFilter = change -> {
            String newText = change.getControlNewText();
            // Allow empty field (so user can delete)
            if (newText.isEmpty()) {return change;}
            try {
                // only a sensible range of time units allowed
                int value = Integer.parseInt(newText);

                if (value > 0 && value < 1000) return change;

            } catch (NumberFormatException ignored) {} // only numbers allowed

            return null; // reject change
        };

        controller.getNquestionsTextfield().setTextFormatter(new TextFormatter<>(nquestionsFilter));
        controller.getTimeTextField().setTextFormatter(new TextFormatter<>(timeFilter));
        controller.getDifficultySlider().setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                return switch (value.intValue()) {
                    case 100 -> "Very easy";
                    case 200 -> "Easy";
                    case 300 -> "Medium";
                    case 400 -> "Hard";
                    case 500 -> "Very hard";
                    default -> controller.getDifficultySlider().getValue() + "";
                };
            }
            @Override
            public Double fromString(String string) {
                return 0.0;
            }
        });
        controller.getDifficultySlider().setShowTickLabels(true);

        controller.getTimeUnitChoiceBox().getItems().addAll("Seconds", "Minutes", "Hours", "No Limit");
        controller.getTimeUnitChoiceBox().setValue("Minutes");
        controller.getTimeTextField().disableProperty().bind(Bindings.equal("No Limit", controller.getTimeUnitChoiceBox().valueProperty()));

        ObservableSet<LittlePopUp.SelectTreeItemResult> selectTreeItemResults = FXCollections.observableSet(new HashSet<>());
        selectTreeItemResults.addListener(new SetChangeListener<LittlePopUp.SelectTreeItemResult>() {
            @Override
            public void onChanged(Change<? extends LittlePopUp.SelectTreeItemResult> change) {
                if (change.wasAdded()) {
                    controller.getTreeItemDisplayHBox().getChildren().add(ReusableUIComponent.createTag(change.getElementAdded(), selectTreeItemResults::remove, "x"));
                }
                if (change.wasRemoved()) {
                    controller.getTreeItemDisplayHBox().getChildren().removeIf(node -> node.getUserData().equals(change.getElementRemoved()));
                }
            }
        });

        List<TreeView<ANode>> treeViews = new ArrayList<>(fromModels.size());   // create treeviews from the models.  (chose not to import the real model treeviews bcuz i'd need to set cellfactory to disable editing which may clash with what the user has enabled in the main window).
        for (Model model : fromModels) {                                        // these trees will be needed every 'Add' call. selectTreeItemResults.contains(SelectTreeItemResult) will not work if selectTreeItemDialog() does not work with the same TreeItems every call.
            TreeViewSetup treeViewSetup = new TreeViewSetup(new UndoableANodeTreeViewEditor());
            TreeView<ANode> treeView = new TreeView<>();
            treeViewSetup.setupTree(treeView, model.getRoot(), false);
            treeViews.add(treeView);
        }

        controller.getAddTreeItemButton().setOnAction(e -> {
            LittlePopUp.SelectTreeItemResult pickTreeItemResult = LittlePopUp.selectTreeItemDialog(treeViews.stream().map(TreeView::getRoot).toList(), fromModels.stream().map(Model::getName).toList(), "Select a TreeItem", "Select a subtree from which the quiz will be generated.");
            if (selectTreeItemResults.contains(pickTreeItemResult)) return;
            selectTreeItemResults.add(pickTreeItemResult);
        });

        controller.getTreeItemDisplayScrollpane().maxWidthProperty().bind(controller.getTreeItemChoosingToolbar().widthProperty().multiply(0.8));   // otherwise it will grow so large it will be clipped


        controller.getAcceptButton().setOnAction(e -> {
            if (selectTreeItemResults.isEmpty()) {
                controller.getNoTreeSelectedLabel().visibleProperty().bind(Bindings.isEmpty(selectTreeItemResults));
                return;
            }
            List<TreeItem<ANode>> roots = selectTreeItemResults.stream().map(LittlePopUp.SelectTreeItemResult::result).toList();
            List<URI> resourceLocations = new ArrayList<>(roots.size());
            for (LittlePopUp.SelectTreeItemResult selectTreeItemResult : selectTreeItemResults) {
                Model model = fromModels.stream().filter(m -> m.getName().equals(selectTreeItemResult.treeName())).toList().getFirst(); //get the model with the matching name
                if (model.getFilesDirURL() != null) {
                    try {resourceLocations.add(model.getFilesDirURL().toURI());} catch (URISyntaxException urie) {logger.log(Level.SEVERE, "failed to parse model.filesURL.toURI", e);}
                } else if (model.getFilesDir() != null && model.getFilesDir().exists()) {
                    resourceLocations.add(model.getFilesDir().toURI());
                }
            }
            Difficulty difficulty =
                    switch ((int) controller.getDifficultySlider().getValue()) {
                case 100 -> Difficulty.VERY_EASY;
                case 200 -> Difficulty.EASY;
                case 300 -> Difficulty.MEDIUM;
                case 400 -> Difficulty.HARD;
                case 500 -> Difficulty.VERY_HARD;
                default -> new Difficulty((int) controller.getDifficultySlider().getValue(), "Custom: " + controller.getDifficultySlider().getValue());
                    };
            int time = controller.getTimeUnitChoiceBox().getValue().equals("No Limit") ? -1 : Integer.parseInt(controller.getTimeTextField().getText());
            TimeUnit timeUnit = switch (controller.getTimeUnitChoiceBox().getValue()) {
                case "Hours" -> TimeUnit.HOURS;
                case "Seconds" -> TimeUnit.SECONDS;
                default -> TimeUnit.MINUTES;
            };
            this.integerUIQuiz = simpleIntQuizFromTree(roots, resourceLocations, Integer.parseInt(controller.getNquestionsTextfield().getText()), difficulty, time,timeUnit, controller.getShowCorrectAnswerCheckbox().isSelected());
            onCreate();
        });


    }

    /**
     *
     * @param roots
     * @param resourceLocations
     * @param nquestions
     * @param difficulty
     * @param time
     * @param showCorrectAnswerOnWrong
     * @param timeUnit
     * @return the UIQuiz
     */
    public UIQuiz<Integer> simpleIntQuizFromTree(List<TreeItem<ANode>> roots, List<URI> resourceLocations, int nquestions, Difficulty difficulty, int time, TimeUnit timeUnit, boolean showCorrectAnswerOnWrong) {
//        TreeItem<ANode> target = TreeAnalysisUtils.getTreeItemWANodeId("FMA7202", roots.getFirst());
        List<UIQuestion<?, Integer>> questions = new ArrayList<>(roots.size());
        int topicInd = 0;
        boolean questSwitch = true;
        for (int i = 0; i < nquestions; i++) {
            TreeItem<ANode> topic = roots.get(topicInd);
            TreeItem<ANode> target = randomTreeItemWFileIDsBelow(topic);
            if (questSwitch) {
                questions.add(makeSimpleMultipleChoice3DQuestion("Question " + i, "Which item is highlighted?", difficulty, target, resourceLocations));
                questSwitch = false;
            }
            else {
                questSwitch = true;
                questions.add(makeSimpleSelectIn3DUIQuestion("Question " + i, "Select the " + target.getValue().name(), difficulty, 0, 1, target, resourceLocations, showCorrectAnswerOnWrong));
            }
            if (topicInd == roots.size()-1) topicInd = 0;
            else topicInd++;
        }
        UIQuiz<Integer> quiz = new UIQuizInt(questions, true, showCorrectAnswerOnWrong, time, timeUnit);
        return quiz;
    }

    public SimpleMultipleChoice3DQuestion<String, Integer> makeSimpleMultipleChoice3DQuestion(String name, String instructions, Difficulty difficulty, TreeItem<ANode> target, List<URI> resourceLocations) {

        Set<TreeItem<ANode>> relativesIncludingTarget = findRelatives2(target, 6);  //TODO: vary n
        List<String> toDraw = new LinkedList<>();
        for (TreeItem<ANode> treeItem : relativesIncludingTarget) {
            toDraw.addAll(treeItem.getValue().fileIds());
        }
        logger.log(Level.CONFIG, "relativesIncTarget: " + relativesIncludingTarget + "\nwhereas toDraw= " + toDraw);
        SimpleMultipleChoice3DQuestion<String, Integer> question = new SimpleMultipleChoice3DQuestion<>(
                difficulty, 0, 1,
                target.getValue().toString(),
                relativesIncludingTarget.stream().map(t -> t.getValue().name()).toList(),
                toDraw,
                target.getValue().fileIds().stream().toList(),
                resourceLocations);
        question.setName(name);
        question.setInstructions(instructions);
        return question;
    }

    public SimpleSelectIn3DUIQuestion<Integer> makeSimpleSelectIn3DUIQuestion(String name, String instructions, Difficulty difficulty, Integer minScore, Integer maxScore, TreeItem<ANode> target, List<URI> resourceLocations, boolean showHintOnWrongAnswer) {
        Set<TreeItem<ANode>> relativesIncludingTarget = findRelatives2(target, 6);  //TODO: vary n
        List<String> toDraw = new LinkedList<>();
        for (TreeItem<ANode> treeItem : relativesIncludingTarget) {
            toDraw.addAll(treeItem.getValue().fileIds());
        }

        SimpleSelectIn3DUIQuestion<Integer> question = new SimpleSelectIn3DUIQuestion<>(
                difficulty, minScore, maxScore,
                target.getValue().fileIds().stream().toList(),
                toDraw,
                resourceLocations,
                showHintOnWrongAnswer
        );
        question.setName(name);
        question.setInstructions(instructions);
        return question;
    }

    private <T> Set<TreeItem<T>> findCloseRelatives(TreeItem<T> item, int n, Function<TreeItem<T>, Boolean> accept) {
        //TODO: ONLY collect those that have fileIDs
        if (n == 1) return Set.of(item);
        Set<TreeItem<T>> result = new HashSet<>(n);
        result.add(item);
        int np = n / 2;
        int nc = n / 2;
        if (np + nc < n) nc++;
        if (item.getParent() != null) {
            if (!item.getChildren().isEmpty()) {
                recParent(item.getParent(), np, result, item, accept);
                recChildren(item, nc, result, accept);
            } else recParent(item.getParent(), np + nc, result, item, accept);
        } else {
            if (!item.getChildren().isEmpty()) {
                recChildren(item, nc + np, result, accept);
            } else return Set.of(item);
        }
        return result;
    }

    private <T> int recParent(TreeItem<T> parent, int n, Set<TreeItem<T>> result, TreeItem<T> childAvoid, Function<TreeItem<T>, Boolean> accept) {
        logger.log(Level.CONFIG, parent.toString() + " n=" + n + " avoid=" + childAvoid.toString() + "\n" + result.toString());
        if (n==0) return 0;
        if (accept.apply(parent) && !result.contains(parent)) {
            result.add(parent);
            n--;
        }
        if (n > 0) {
            int np = n / 2;
            int nc = n / 2;
            if (np + nc != n) nc++;
            int leftOvernp = np;
            if (parent.getParent() != null) {
                leftOvernp = recParent(parent.getParent(), leftOvernp, result, parent, accept);
            }
            int leftOvernc = nc;
            if (nc <= parent.getChildren().stream().filter(accept::apply).toList().size()) {
                for (TreeItem<T> child : parent.getChildren()) {
                    if (nc == 0) break;
                    if (child == childAvoid || !accept.apply(child)) continue;
                    result.add(child);
                    nc--;
                }
            } else {
                int ndiv = nc / parent.getChildren().stream().filter(accept::apply).toList().size();
                int leftOver = 0;
                for (int i = 0; i < parent.getChildren().size(); i++) {
                    if (parent.getChildren().get(i) == childAvoid || !accept.apply(parent.getChildren().get(i))) continue;

//                    if (i == parent.getChildren().size()) if ((double) nc / parent.getChildren().size() > nc / parent.getChildren().size()) ndiv += nc - ndiv * parent.getChildren().size();
//                    if (result.contains(parent.getChildren().get(i))) {
//                        if (!parent.getChildren().get(i).getChildren().isEmpty()) leftOver = recChildren(parent.getChildren().get(i).getChildren().getFirst(), ndiv + leftOver, result, accept);
//                        else continue;
//                    } else leftOver = recChildren(parent.getChildren().get(i), ndiv, result, accept);
                    recChildren(parent.getChildren().get(i), ndiv, result, accept);
                }
                leftOvernc = leftOver;
            }
            if (leftOvernp != 0) {
                if (leftOvernc != 0) return leftOvernc + leftOvernp;
                else {
                    return recChildren(parent, leftOvernp, result, accept);
                }
            } else {
                if (leftOvernc != 0) {
                    if (parent.getParent() != null ) return recParent(parent.getParent(), leftOvernc, result, parent, accept);
                }
                return leftOvernc;
            }
        } else return 0;
    }

    private <T> int recChildren(TreeItem<T> child, int n, Set<TreeItem<T>> acc, Function<TreeItem<T>, Boolean> accept) {
        logger.log(Level.CONFIG, child.toString() + " n=" + n + "\n" + acc.toString());
        if (n==0) return 0;
        if (accept.apply(child)) {
            acc.add(child);
            n--;
        }
        if (n > 0) {
            if (n <= child.getChildren().size()) {
                for (TreeItem<T> grandChild : child.getChildren()) {
                    acc.add(grandChild);
                    n--;
                    if (n==0) break;
                }
                return 0;
            } else {
                int ndiv = n / child.getChildren().size();
                int leftOvern = 0;
                for (int i = 0; i < child.getChildren().size(); i++) {
                    if (i == child.getChildren().size()) if ((double) n / child.getChildren().size() > n / child.getChildren().size()) ndiv += n - ndiv * child.getChildren().size();
                    if (acc.contains(child.getChildren().get(i))) {
                        if (!child.getChildren().get(i).getChildren().isEmpty()) leftOvern = recChildren(child.getChildren().get(i).getChildren().getFirst(), ndiv + leftOvern, acc, accept);
                        else continue;
                    } else {
                        leftOvern = recChildren(child.getChildren().get(i), ndiv + leftOvern, acc, accept);
                    }
                }
                return leftOvern;   //falsch, aber vllt das nächstbeste.
            }
        }
        return 0;
    }

    private final float np_fraction = 0.25f;

    private Set<TreeItem<ANode>> findRelatives2(TreeItem<ANode> root, int n) {
        Set<TreeItem<ANode>> result = new HashSet<>(n);
        Function<TreeItem<ANode>, Boolean> accept = (t) -> !t.getValue().getFileIds().isEmpty() && !result.contains(t);
        int np = (int) (n*np_fraction);
        int nc = n - np;

        int leftc = recChildren2(root, nc, result, accept);
        np += leftc;
        int leftp = recParent2(root, np, result, accept);

        if (leftp > 0 && leftc == 0) recChildren2(root, leftc, result, accept);

        return result;
    }

    private <T> int recParent2(TreeItem<T> item, int n, Set<TreeItem<T>> result, Function<TreeItem<T>, Boolean> accept) {
        logger.log(Level.CONFIG, item + "|" + n + "|" + result);
        if (n < 1) return 0;
        if (item.getParent() == null) return n;
        TreeItem<T> parent = item.getParent();
        if (accept.apply(parent)) {
            result.add(parent);
            logger.log(Level.CONFIG, "adding " + parent.getValue().toString());
            n--;
        }
        if (n < 1) return 0;
//        Function<TreeItem<T>, Boolean> acceptAndIgnoreThis = new Function<TreeItem<T>, Boolean>() { //funktioniert? weil die kinder von tTreeItem ja trotzdem angeschaut werden?
//            @Override
//            public Boolean apply(TreeItem<T> tTreeItem) {
//                return (tTreeItem != item) && accept.apply(tTreeItem);
//            }
//        };
        if (parent.getParent() == null) return recChildren2(parent, n, result, accept); //AndIgnoreThis

        int np = (int) (n * np_fraction);
        int nc = n-np;
        int leftc = recChildren2(parent, nc, result, accept);   //AndIgnoreThis
        np += leftc;
        int leftp = recParent2(parent, np, result, accept);
        if (leftp > 0 && leftc > 0) return leftp + leftc;
        if (leftp > 0) return recChildren2(parent, leftp, result, t -> !result.contains(t) && accept.apply(t)); //AndIgnoreThis
        if (leftc > 0) return recParent2(parent, leftc, result, t -> !result.contains(t) && accept.apply(t));
        return 0;
    }

    private <T> int recChildren2(TreeItem<T> item, int n, Set<TreeItem<T>> result, Function<TreeItem<T>,Boolean> acccept) {
        logger.log(Level.CONFIG, item + "|" + n + "|" + result);
        if (n < 1) return 0;
        if (acccept.apply(item)) {
            result.add(item);
            logger.log(Level.CONFIG, "adding " + item.getValue().toString());
            n--;
        }
        if (n < 1) return 0;
        if (item.getChildren().isEmpty()) return n;
        LinkedList<TreeItem<T>> children = sortChildrenByAcceptCount(item, acccept);

        children.removeIf(treeItem -> {
            SimpleIntegerProperty acceptCount1 = new SimpleIntegerProperty(0);
            TreeAnalysisUtils.applyRec(treeItem, t -> {if (acccept.apply(t)) {acceptCount1.set(acceptCount1.getValue() + 1);} return true;});
            return acceptCount1.get() == 0;
        });
        if (children.isEmpty()) {
            logger.log(Level.CONFIG, "children is empty after cleanup");
            return n;
        }

        Project.Util.Utils.shuffleList(children);

        int leftn = 0;
        int ndiv = n / children.size();
        int ncount = 0;

        for (int i = 0; i < children.size(); i++) {
            if (ncount == n) break;
            if (ncount + (children.size() - i) * ndiv < n) {
                logger.log(Level.CONFIG, ncount + " + (" + children.size() + "-" + i + ") * " + ndiv + "<" + n + " -> " +leftn + "++");
                leftn++;
            }
            int nsoll = ndiv + leftn;
            int nleft = recChildren2(children.get(i), nsoll, result, acccept);
            logger.log(Level.CONFIG,"nsoll="+nsoll + " nleft="+nleft);
            ncount += nsoll - nleft; // ncount keeps track of how many things have been added to result. nsoll = how many need to be added, nleft = how many could not be added.
            leftn = nleft;
        }
        return leftn;
    }

    private static <T> @NotNull LinkedList<TreeItem<T>> sortChildrenByAcceptCount(TreeItem<T> item, Function<TreeItem<T>, Boolean> acccept) {
        LinkedList<TreeItem<T>> children = new LinkedList<>(item.getChildren());
        children.sort(new Comparator<TreeItem<T>>() {
            @Override
            public int compare(TreeItem<T> o1, TreeItem<T> o2) {
                SimpleIntegerProperty acceptCount1 = new SimpleIntegerProperty(0);
                SimpleIntegerProperty acceptCount2 = new SimpleIntegerProperty(0);
                TreeAnalysisUtils.applyRec(o1, t -> {if (acccept.apply(t)) acceptCount1.add(1); return true;});
                TreeAnalysisUtils.applyRec(o2, t -> {if (acccept.apply(t)) acceptCount2.add(1); return true;});
                return Integer.compare(acceptCount1.get(), acceptCount2.get());
            }
        });
        return children;
    }



    private static class RandomUIQuizGeneratorView {
        private final Parent root;
        private final QuizPropertyChooserController controller;
        public Parent getRoot() {return root;}
        public QuizPropertyChooserController getController() {return controller;}

        public RandomUIQuizGeneratorView() throws IOException {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Project/FXML/QuizPropertyChooser.fxml"));
            this.root = loader.load();
            this.controller = loader.getController();
        }
    }

    private TreeItem<ANode> randomTreeItemWFileIDsBelow(TreeItem<ANode> root) {
        List<TreeItem<ANode>> applicable = new LinkedList<>();
        TreeAnalysisUtils.applyRec(root, t -> {if (!t.getValue().getFileIds().isEmpty()) applicable.add(t); return true;});
        return applicable.get(new Random().nextInt(0, applicable.size()));
    }

}