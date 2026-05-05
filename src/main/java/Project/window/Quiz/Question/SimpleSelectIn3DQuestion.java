package Project.window.Quiz.Question;

import Project.window.Quiz.Difficulty;
import Project.window.ThreeDPaneHandling.ThreeDGroupHandler;
import javafx.scene.Node;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;

public abstract class SimpleSelectIn3DQuestion<S extends Comparable<S>> extends SimpleMultipleChoiceQuestion<Node, String, S> implements Needs3DAccess {

    protected ThreeDGroupHandler threeDGroupHandler;

    protected List<URI> resourceLocations;

    protected final List<String> idsToDraw;

    /**
     * A simple 3D question model.
     * <P>
     * Implement evaluate().
     *
     * @param difficulty    difficulty of the question
     * @param minScore      minimum score
     * @param maxScore      max score
     * @param correctAnswer a list of one or more IDs of MeshViews that represent the correct answer
     * @param idsToDraw IDs of MeshViews that will be drawn upon calling ask()
     * @param resourceLocations
     */
    public SimpleSelectIn3DQuestion(Difficulty difficulty, S minScore, S maxScore, List<String> correctAnswer, List<String> idsToDraw, List<URI> resourceLocations) {
        super(difficulty, minScore, maxScore, correctAnswer, idsToDraw);
        this.idsToDraw = idsToDraw;
        this.resourceLocations = resourceLocations;
    }

    @Override
    public void giveAccess(ThreeDGroupHandler threeDGroupHandler) {
        this.threeDGroupHandler = threeDGroupHandler;
        HashSet<String> allFileIDs = new HashSet<>();
        for (String id : idsToDraw) if (this.threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(id) == null) allFileIDs.add(id);
        for (URI uri : this.resourceLocations) {
            try {
                this.threeDGroupHandler.getHasFXGroupContentsContainer().addResourceLocation(uri.toURL(), allFileIDs);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to load resource location " + uri.toString());  //TODO: must be caught by Quiz
            }
        }
    }

}
