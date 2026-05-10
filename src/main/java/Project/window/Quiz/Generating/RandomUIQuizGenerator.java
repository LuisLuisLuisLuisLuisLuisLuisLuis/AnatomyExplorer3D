package Project.window.Quiz.Generating;

import Project.model.ANode;
import Project.model.Model;
import Project.window.PopUp.LittlePopUp;
import Project.window.Quiz.*;
import Project.window.Quiz.Question.*;
import Project.window.Slicing.Plane;
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

    private final Random random;

    /**
     * @param fromModels Models from which the Quiz is to be generated.
     */
    public RandomUIQuizGenerator(List<Model> fromModels) {

        RandomUIQuizGeneratorView randomUIQuizGeneratorView = null;
        this.random = new Random();

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
                // only 1-30 questions allowed
                int value = Integer.parseInt(newText);

                if (value > 0 && value < 31) return change;

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
//        controller.getDifficultySlider().setLabelFormatter(new StringConverter<Double>() {    // when the slider still encoded difficulty
//            @Override
//            public String toString(Double value) {
//                return switch (value.intValue()) {
//                    case 100 -> "Very easy";
//                    case 200 -> "Easy";
//                    case 300 -> "Medium";
//                    case 400 -> "Hard";
//                    case 500 -> "Very hard";
//                    default -> controller.getDifficultySlider().getValue() + "";
//                };
//            }
//            @Override
//            public Double fromString(String string) {
//                return 0.0;
//            }
//        });
        controller.getDifficultySlider().setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return "" + object.intValue();
            }

            @Override
            public Double fromString(String string) {
                return 0.0;
            }
        });
        controller.getDifficultySlider().setShowTickLabels(true);

        controller.getTimeUnitChoiceBox().getItems().addAll("Seconds", "Minutes", "Hours", "No Limit");
        controller.getTimeTextField().disableProperty().bind(Bindings.equal("No Limit", controller.getTimeUnitChoiceBox().valueProperty()));
        controller.getTimeUnitChoiceBox().setValue("No Limit");

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
            treeView.setId(model.getName());
        }

        controller.getAddTreeItemButton().setOnAction(e -> {
            LittlePopUp.SelectTreeItemResult pickTreeItemResult = LittlePopUp.selectTreeItemDialog(treeViews.stream().map(TreeView::getRoot).toList(), fromModels.stream().map(Model::getName).toList(), "Select a TreeItem", "Select a subtree from which the quiz will be generated:");
            if (selectTreeItemResults.contains(pickTreeItemResult) || pickTreeItemResult == null || pickTreeItemResult.result() == null) return;
            selectTreeItemResults.add(pickTreeItemResult);
        });

        controller.getTreeItemDisplayScrollpane().maxWidthProperty().bind(controller.getTreeItemChoosingToolbar().widthProperty().multiply(0.7));   // otherwise it will grow so large it will be clipped


        controller.getAcceptButton().setOnAction(e -> {
            if (selectTreeItemResults.isEmpty()) {
                controller.getNoTreeSelectedLabel().visibleProperty().bind(Bindings.isEmpty(selectTreeItemResults));
                return;
            }
            List<TreeItem<ANode>> roots = selectTreeItemResults.stream().map(LittlePopUp.SelectTreeItemResult::result).toList();
//            List<URI> resourceLocations = new ArrayList<>(roots.size());
            Map<TreeItem<ANode>, Model> topicToModel = new HashMap<>();

            for (LittlePopUp.SelectTreeItemResult selectTreeItemResult : selectTreeItemResults) {
                Model model = fromModels.stream().filter(m -> m.getName().equals(selectTreeItemResult.treeName())).toList().getFirst(); //get the model with the matching name
                topicToModel.put(selectTreeItemResult.result(), model);
//                if (model.getResourceFilesDir() != null) {
//                    try {resourceLocations.add(model.getResourceFilesDir().toURI());} catch (URISyntaxException urie) {logger.log(Level.SEVERE, "failed to parse model.filesURL.toURI", e);}
//                } else if (model.getFilesDir() != null && model.getFilesDir().exists()) {
//                    resourceLocations.add(model.getFilesDir().toURI());
//                }
            }

//            Difficulty difficulty =
//                    switch ((int) controller.getDifficultySlider().getValue()) {
//                case 100 -> Difficulty.VERY_EASY;
//                case 200 -> Difficulty.EASY;
//                case 300 -> Difficulty.MEDIUM;
//                case 400 -> Difficulty.HARD;
//                case 500 -> Difficulty.VERY_HARD;
//                default -> new Difficulty((int) controller.getDifficultySlider().getValue(), "Custom: " + (int) controller.getDifficultySlider().getValue());
//            };

            HOW_MUCH_TO_DRAW = (int) controller.getDifficultySlider().getValue() + 1;   //+1 because at least the target needs to be drawn

            int time = controller.getTimeUnitChoiceBox().getValue().equals("No Limit") ? -1 : Integer.parseInt(controller.getTimeTextField().getText());
            TimeUnit timeUnit = switch (controller.getTimeUnitChoiceBox().getValue()) {
                case "Hours" -> TimeUnit.HOURS;
                case "Seconds" -> TimeUnit.SECONDS;
                default -> TimeUnit.MINUTES;
            };
            this.integerUIQuiz = simpleIntQuizFromTree(topicToModel, Integer.parseInt(controller.getNquestionsTextfield().getText()), null, time,timeUnit, controller.getShowCorrectAnswerCheckbox().isSelected());
            onCreate();
        });

        // find treeItems of aorta, vena cava
        for (Model model : fromModels) {
            if (model.getName().equals("anatomy")) {
                logger.log(Level.CONFIG, "main model exists");
                List<TreeView<ANode>> mainTreeViewMaybe = treeViews.stream().filter(t -> t.getId().equals("anatomy")).toList();
                if (mainTreeViewMaybe.isEmpty()) break;
                logger.log(Level.CONFIG, "treeview of main model exists");
                //setting some of the needed treeItems

                ANode aortaANode = TreeAnalysisUtils.getANodeWithName("aorta", model.getRoot());
                ANode supVenaCavaANode = TreeAnalysisUtils.getANodeWithName("superior vena cava", model.getRoot());
                ANode infVenaCavaANode = TreeAnalysisUtils.getANodeWithName("inferior vena cava", model.getRoot());
                ANode respiratoryANode = TreeAnalysisUtils.getANodeWithName("respiratory system", model.getRoot());
                ANode linfPulmVeinNode = TreeAnalysisUtils.getANodeWithName("left inferior pulmonary vein", model.getRoot());
                ANode rinfPulmVeinNode = TreeAnalysisUtils.getANodeWithName("right inferior pulmonary vein", model.getRoot());
                ANode lsupPulmVeinNode = TreeAnalysisUtils.getANodeWithName("left superior pulmonary vein", model.getRoot());
                ANode rsupPulmVeinNode = TreeAnalysisUtils.getANodeWithName("right superior pulmonary vein", model.getRoot());
                ANode pulmTrunkNode = TreeAnalysisUtils.getANodeWithName("pulmonary trunk", model.getRoot());
                ANode hepPortalNode = TreeAnalysisUtils.getANodeWithName("hepatic portal vein", model.getRoot());
                ANode prehepPortalNode = TreeAnalysisUtils.getANodeWithName("pre-hepatic portal vein", model.getRoot());

                if (aortaANode != null) aortaTreeItem = TreeAnalysisUtils.getTreeItemWANodeId(aortaANode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (supVenaCavaANode != null) superiorVenaCava = TreeAnalysisUtils.getTreeItemWANodeId(supVenaCavaANode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (infVenaCavaANode != null) inferiorVenaCava = TreeAnalysisUtils.getTreeItemWANodeId(infVenaCavaANode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (respiratoryANode != null) respiratorySystem = TreeAnalysisUtils.getTreeItemWANodeId(respiratoryANode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (pulmTrunkNode != null) pulmonaryTrunk = TreeAnalysisUtils.getTreeItemWANodeId(pulmTrunkNode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (linfPulmVeinNode != null) linfPulmVein = TreeAnalysisUtils.getTreeItemWANodeId(linfPulmVeinNode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (rinfPulmVeinNode != null) rinfPulmVein = TreeAnalysisUtils.getTreeItemWANodeId(rinfPulmVeinNode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (lsupPulmVeinNode != null) lsupPulmVein = TreeAnalysisUtils.getTreeItemWANodeId(lsupPulmVeinNode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (rsupPulmVeinNode != null) rsupPulmVein = TreeAnalysisUtils.getTreeItemWANodeId(rsupPulmVeinNode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (hepPortalNode != null) hepaticPortal = TreeAnalysisUtils.getTreeItemWANodeId(hepPortalNode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());
                if (prehepPortalNode != null) prehepPortal = TreeAnalysisUtils.getTreeItemWANodeId(prehepPortalNode.conceptId(), mainTreeViewMaybe.getFirst().getRoot());

                break;
            }
        }


    }

    /**
     * @param target A treeItem
     * @return The types of questions this class can make for that treeItem
     */
    private List<AllQuestionTypes> getQuestTypesForTarget(TreeItem<ANode> target) {
        if (forbiddenIDs.contains(target.getValue().conceptId())) return List.of();
        List<AllQuestionTypes> result = new LinkedList<>();
        result.add(AllQuestionTypes.IDENTIFY_IN_3D);
        if (!forbiddenIDsForSelectIn3D.contains(target.getValue().conceptId()) && !TreeAnalysisUtils.isPartOfTree(respiratorySystem, target)) result.add(AllQuestionTypes.SELECT_IN_3D);  // resp system has many (leaf) nodes with broad names that still have fileIDs because
                                                                                                                    // they represent an important category. but they are difficult to pin down in such a selection question.
        for (TreeItem<ANode> tI : List.of(aortaTreeItem, pulmonaryTrunk)) {
            if (tI != null) {
                if (TreeAnalysisUtils.isPartOfTree(tI, target)) {
                    if (!target.getChildren().isEmpty()) result.add(AllQuestionTypes.ARTERY_BRANCH);
                    if (target != tI) result.add(AllQuestionTypes.ARTERY_SOURCE);
                    return result;
                }
            }
        }

        for (TreeItem<ANode> tI : List.of(inferiorVenaCava, superiorVenaCava, linfPulmVein, rinfPulmVein, lsupPulmVein, rsupPulmVein, hepaticPortal, prehepPortal)) {
            if (tI != null) {
                if (TreeAnalysisUtils.isPartOfTree(tI, target)) {
                    if (!target.getChildren().isEmpty()) result.add(AllQuestionTypes.VEIN_SOURCE);
                    if (target != tI) result.add(AllQuestionTypes.VEIN_DRAIN);
                    return result;
                }
            }
        }
        if (target.getValue().fileIds().size() == 1) result.add(AllQuestionTypes.IDENTIFY_IN_SLICE);
        return result;
    }

    /**
     * Possible types of questions this class can make.
     */
    private enum AllQuestionTypes {
        IDENTIFY_IN_3D,
        SELECT_IN_3D,
        IDENTIFY_IN_SLICE,
        VEIN_DRAIN,
        VEIN_SOURCE,
        ARTERY_SOURCE,
        ARTERY_BRANCH
    }

    // all of those can qualify for vein/artery questions, but only in downwards direction because they have no parent vessel.
    private TreeItem<ANode> aortaTreeItem = null;
    private TreeItem<ANode> inferiorVenaCava = null;
    private TreeItem<ANode> superiorVenaCava = null;
    private TreeItem<ANode> respiratorySystem = null;
    private TreeItem<ANode> linfPulmVein = null;
    private TreeItem<ANode> rinfPulmVein = null;
    private TreeItem<ANode> lsupPulmVein = null;
    private TreeItem<ANode> rsupPulmVein = null;
    private TreeItem<ANode> pulmonaryTrunk = null;
    private TreeItem<ANode> hepaticPortal = null;
    private TreeItem<ANode> prehepPortal = null;


    /** these must not be target for select in 3d questions*/
    private final Set<String> forbiddenIDsForSelectIn3D = Set.of("FJ1913");

    /** these must not be drawn at all*/
    private final Set<String> forbiddenIDs; //for now only skin. reason: it hides everything else.
    {
        Set<String> result = HashSet.newHashSet(2);
        result.add("FJ2810");
        forbiddenIDs = result;
    }

    /** Default number of options in a multiple choice question*/
    private final int DEFAULT_NO_MC_OPTIONS = 6;

    private int HOW_MUCH_TO_DRAW = 12;

    private int difficultyToN(Difficulty difficulty) {
        return 12 - difficulty.getDifficultyLevel() / 100;
    }

    /**
     * @param topicToModel maps target TreeItems to their Model of origin. Needed to provide each question with the appropriate
     *                      resource location.
     * @param nquestions
     * @param difficulty
     * @param time
     * @param showCorrectAnswerOnWrong
     * @param timeUnit
     * @return the UIQuiz
     */
    public UIQuiz<Integer> simpleIntQuizFromTree(Map<TreeItem<ANode>, Model> topicToModel, int nquestions, Difficulty difficulty, int time, TimeUnit timeUnit, boolean showCorrectAnswerOnWrong) {
        List<UIQuestion<?, Integer>> questions = new ArrayList<>(topicToModel.size());
        for (TreeItem<ANode> root : new LinkedList<>(topicToModel.keySet())) if (root.getChildren().isEmpty() && forbiddenIDs.contains(root.getValue().conceptId())) topicToModel.remove(root);
        int topicInd = 0;
        int sliceAxisInd = 0;

        HashMap<AllQuestionTypes, Integer> countQuestTypes = new HashMap<>(AllQuestionTypes.values().length);   // keep track of how often each available question type occurs
        for (AllQuestionTypes qType : AllQuestionTypes.values()) countQuestTypes.put(qType, 0);

        List<TreeItem<ANode>> topics = new ArrayList<>(topicToModel.keySet());

        for (int i = 0; i < nquestions; i++) {
            String questionName = "Question " + (i+1);
            TreeItem<ANode> topic = topics.get(topicInd);
            TreeItem<ANode> target = randomTreeItemWFileIDsBelow(topic);

            List<AllQuestionTypes> possibleQuestTypes = new ArrayList<>(getQuestTypesForTarget(target));
            if (possibleQuestTypes.isEmpty()) {
                i--;
                continue;
            }
            possibleQuestTypes.sort(Comparator.comparingInt(countQuestTypes::get));
            AllQuestionTypes qType = possibleQuestTypes.getFirst(); // choose the most underrepresented question type

            switch (qType) {
                case VEIN_DRAIN -> questions.add(makeVeinDrainQuestion(target, questionName, difficulty, 0,1));
                case ARTERY_SOURCE -> questions.add(makeArterySourceQuestion(target, questionName, difficulty, 0,1));
                case VEIN_SOURCE -> questions.add(makeVeinSourceQuestion(target, questionName, difficulty, 0,1));
                case ARTERY_BRANCH -> questions.add(makeArteryBranchQuestion(target, questionName, difficulty, 0,1));
                case IDENTIFY_IN_3D -> questions.add(makeSimpleMultipleChoice3DQuestion(questionName, "Which item is highlighted?", difficulty, target, topicToModel.get(topic)));
                case SELECT_IN_3D -> questions.add(makeSimpleSelectIn3DUIQuestion(questionName, "Select the " + target.getValue().name(), difficulty, 0, 1, target, showCorrectAnswerOnWrong, topicToModel.get(topic)));
                case IDENTIFY_IN_SLICE -> {
                    Plane.BodyAxis bodyAxis = switch (sliceAxisInd){case 0 -> Plane.BodyAxis.AXIAL; case 1 -> Plane.BodyAxis.SAGGITAL; default -> Plane.BodyAxis.CORONAL;};
                    if (sliceAxisInd == 2) sliceAxisInd = 0; else sliceAxisInd++;
                    questions.add(makeSimpleSliceQuestion(questionName, "Which item is highlighted?", difficulty, target, bodyAxis, random.nextBoolean(), topicToModel.get(topic)));
                }
            }
            countQuestTypes.put(qType, countQuestTypes.get(qType) + 1);

            if (topicInd == topicToModel.size()-1) topicInd = 0; else topicInd++;

        }

        UIQuiz<Integer> quiz = new UIQuizInt(questions, true, showCorrectAnswerOnWrong, time, timeUnit);
        return quiz;
    }


    public SimpleMultipleChoice3DQuestion<String, Integer> makeSimpleMultipleChoice3DQuestion(String name, String instructions, Difficulty difficulty, TreeItem<ANode> target, Model model) {

        Set<TreeItem<ANode>> relativesIncludingTarget = findRelatives2(target, difficulty !=  null ? difficultyToN(difficulty) : HOW_MUCH_TO_DRAW);
        List<String> toDraw = new LinkedList<>();
        for (TreeItem<ANode> treeItem : relativesIncludingTarget) {
            toDraw.addAll(treeItem.getValue().fileIds());
        }

        String correctAnswer = target.getValue().name();
        List<String> possibleOptions = new ArrayList<>(DEFAULT_NO_MC_OPTIONS);
        possibleOptions.addAll(relativesIncludingTarget.stream().map(t -> t.getValue().name()).toList().subList(0, Math.min(DEFAULT_NO_MC_OPTIONS, relativesIncludingTarget.size())));
        if (!possibleOptions.contains(correctAnswer)) {
            possibleOptions.removeFirst();
            possibleOptions.add(correctAnswer);
        }

        logger.log(Level.CONFIG, "relativesIncTarget: " + relativesIncludingTarget + "\nwhereas toDraw= " + toDraw);
        SimpleMultipleChoice3DQuestion<String, Integer> question = new SimpleMultipleChoice3DQuestion<>(
                difficulty, 0, 1,
                target.getValue().toString(),
                possibleOptions, //                relativesIncludingTarget.stream().map(t -> t.getValue().name()).toList(),
                toDraw,
                target.getValue().fileIds().stream().toList(),
                true
        );
        if (model.getResourceFilesDir() != null) question.setResourceLocations(List.of(model.getResourceFilesDir()));
        else if (model.getFilesDir() != null) question.setFileDirs(List.of(model.getFilesDir()));
        question.setName(name);
        question.setInstructions(instructions);
        return question;
    }

    public SimpleSelectIn3DUIQuestion<Integer> makeSimpleSelectIn3DUIQuestion(String name, String instructions, Difficulty difficulty, Integer minScore, Integer maxScore, TreeItem<ANode> target, boolean showHintOnWrongAnswer, Model model) {

        Set<TreeItem<ANode>> relativesIncludingTarget = findRelatives2(target, difficulty != null ? difficultyToN(difficulty) : HOW_MUCH_TO_DRAW);
        List<String> toDraw = new LinkedList<>();
        for (TreeItem<ANode> treeItem : relativesIncludingTarget) {
            toDraw.addAll(treeItem.getValue().fileIds());
        }
        logger.log(Level.CONFIG, "relativesIncTarget: " + relativesIncludingTarget + "\nwhereas toDraw= " + toDraw);
        SimpleSelectIn3DUIQuestion<Integer> question = new SimpleSelectIn3DUIQuestion<>(
                difficulty, minScore, maxScore,
                target.getValue().fileIds().stream().toList(),
                toDraw,
                showHintOnWrongAnswer
        );
        if (model.getResourceFilesDir() != null) question.setResourceLocations(List.of(model.getResourceFilesDir()));
        else if (model.getFilesDir() != null) question.setFileDirs(List.of(model.getFilesDir()));
        question.setName(name);
        question.setInstructions(instructions);
        return question;
    }

    public SimpleSliceMCQuestion<String, Integer> makeSimpleSliceQuestion(String name, String instructions, Difficulty difficulty, TreeItem<ANode> target, Plane.BodyAxis sliceAxis, boolean positiveDir, Model model) {

        //get the names of the target and some other nodes for multiple choice
        List<String> namesForMCOptions = findRelatives2(target, DEFAULT_NO_MC_OPTIONS).stream().map(t -> t.getValue().getName()).toList();

        Set<String> toDraw = new HashSet<>(target.getValue().fileIds());    //will hold all fileIDs of the tree

        TreeItem<ANode> root = new Function<TreeItem<ANode>, TreeItem<ANode>>(){
            @Override
            public TreeItem<ANode> apply(TreeItem<ANode> treeItem) {
                return treeItem.getParent() == null ? treeItem : this.apply(treeItem.getParent());
            }
        }.apply(target);    //find root of tree

        TreeAnalysisUtils.applyRec(root, (t -> {toDraw.addAll(t.getValue().fileIds()); return null;})); //accumulate all fileIDs of the tree

        SimpleSliceMCQuestion<String, Integer> question = new SimpleSliceMCQuestion<>(
                difficulty, 0, 1,
                target.getValue().name(),
                namesForMCOptions,
                toDraw.stream().toList(),
                target.getValue().fileIds().stream().toList(),
                true
        );
        if (model.getResourceFilesDir() != null) question.setResourceLocations(List.of(model.getResourceFilesDir()));
        else if (model.getFilesDir() != null) question.setFileDirs(List.of(model.getFilesDir()));

        question.setName(name);
        question.setInstructions(instructions);
        question.setSliceTarget(target.getValue().fileIds().stream().toList().getFirst());  //only one can be the target
        question.setSliceAxis(sliceAxis, positiveDir);

        return question;
    }


    public SimpleMultipleChoiceUIQuestion<String, Integer> makeVeinDrainQuestion(TreeItem<ANode> target, String name, Difficulty difficulty, Integer minScore, Integer maxScore) {

        List<String> possibleAnswers = new ArrayList<>(findRelatives2(target, DEFAULT_NO_MC_OPTIONS+1).stream().map(t -> t.getValue().name()).toList());
        possibleAnswers.remove(target.getValue().name());
        String correctAnswer = target.getParent().getValue().name();
        if (!possibleAnswers.contains(correctAnswer)) {
            possibleAnswers.removeLast();
            possibleAnswers.add(correctAnswer);
        }
        SimpleMultipleChoiceUIQuestion<String, Integer> question = new SimpleMultipleChoiceUIQuestion<>(
                difficulty, minScore, maxScore, correctAnswer, possibleAnswers, true
        );
        question.setName(name);
        question.setInstructions("Where does the " + target.getValue().name() + " drain to?");
        return question;
    }

    public SimpleMultipleChoiceUIQuestion<String, Integer> makeArterySourceQuestion(TreeItem<ANode> target, String name, Difficulty difficulty, Integer minScore, Integer maxScore) {

        List<String> possibleAnswers = new ArrayList<>(findRelatives2(target, DEFAULT_NO_MC_OPTIONS - 1).stream().map(t -> t.getValue().name()).toList());
        String correctAnswer = target.getParent().getValue().name();
        if (!possibleAnswers.contains(correctAnswer)) {
            possibleAnswers.removeLast();
            possibleAnswers.add(correctAnswer);
        }
        SimpleMultipleChoiceUIQuestion<String, Integer> question = new SimpleMultipleChoiceUIQuestion<>(
                difficulty, minScore, maxScore, correctAnswer, possibleAnswers, true
        );
        question.setName(name);
        question.setInstructions("What's the source of the " + target.getValue().name() + "?");
        return question;
    }

    public SimpleMultipleChoiceUIQuestion<String, Integer> makeArteryBranchQuestion(TreeItem<ANode> target, String name, Difficulty difficulty, Integer minScore, Integer maxScore) {
        List<String> possibleAnswers = findRelatives2(target, DEFAULT_NO_MC_OPTIONS).stream().map(t -> t.getValue().name()).toList();
        List<String> correctAnswers = new LinkedList<>();
        for (TreeItem<ANode> child : target.getChildren()) if (possibleAnswers.contains(child.getValue().name())) correctAnswers.add(child.getValue().name());

        SimpleMultipleChoiceUIQuestion<String, Integer> question = new SimpleMultipleChoiceUIQuestion<>(
                difficulty, minScore, maxScore, correctAnswers, possibleAnswers, false
        );
        question.setName(name);
        question.setInstructions("What does the " + target.getValue().name() + " branch into?");
        return question;
    }

    public SimpleMultipleChoiceUIQuestion<String, Integer> makeVeinSourceQuestion(TreeItem<ANode> target, String name, Difficulty difficulty, Integer minScore, Integer maxScore) {
        List<String> possibleAnswers = findRelatives2(target, DEFAULT_NO_MC_OPTIONS).stream().map(t -> t.getValue().name()).toList();
        List<String> correctAnswers = new LinkedList<>();
        for (TreeItem<ANode> child : target.getChildren()) if (possibleAnswers.contains(child.getValue().name())) correctAnswers.add(child.getValue().name());

        SimpleMultipleChoiceUIQuestion<String, Integer> question = new SimpleMultipleChoiceUIQuestion<>(
                difficulty, minScore, maxScore, correctAnswers, possibleAnswers, false
        );
        question.setName(name);
        question.setInstructions("Which veins drain into the " + target.getValue().name() + "?");
        return question;
    }




    private final float np_fraction = 0.25f;

    private Set<TreeItem<ANode>> findRelatives2(TreeItem<ANode> root, int n) {
        Set<TreeItem<ANode>> result = new HashSet<>(n);
        Function<TreeItem<ANode>, Boolean> accept = (t) -> !t.getValue().getFileIds().isEmpty() && !result.contains(t) && !forbiddenIDs.contains(t.getValue().conceptId());
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

        if (parent.getParent() == null) return recChildren2(parent, n, result, accept); 

        int np = (int) (n * np_fraction);
        int nc = n-np;
        int leftc = recChildren2(parent, nc, result, accept);   
        np += leftc;
        int leftp = recParent2(parent, np, result, accept);
        if (leftp > 0 && leftc > 0) return leftp + leftc;
        if (leftp > 0) return recChildren2(parent, leftp, result, t -> !result.contains(t) && accept.apply(t)); 
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
            if (ncount + (children.size() - i) * (ndiv + leftn) < n) {  // helps if more is necessary or in case casting ndiv to int causes it to round to zero
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
        return applicable.get(random.nextInt(0, applicable.size()));
    }

}