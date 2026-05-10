package Project.window.Quiz.Question;

import Project.window.Quiz.Difficulty;
import Project.window.ThreeDPaneHandling.ThreeDGroupHandler;
import javafx.scene.Node;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;

public abstract class SimpleSelectIn3DQuestion<S extends Comparable<S>> extends SimpleMultipleChoiceQuestion<Node, String, S> implements Needs3DAccess {

    protected ThreeDGroupHandler threeDGroupHandler;

    protected List<String> resourceLocations = List.of();

    protected List<File> fileDirs = List.of();

    /** Provide a list of directories in resources. Will be accessed via getClass().getResource(...) */
    public void setResourceLocations(List<String> resourceDirs) {
        this.resourceLocations = resourceDirs;
    }

    public void setFileDirs(List<File> dirs) {
        this.fileDirs = dirs;
    }

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
     */
    public SimpleSelectIn3DQuestion(Difficulty difficulty, S minScore, S maxScore, List<String> correctAnswer, List<String> idsToDraw) {
        super(difficulty, minScore, maxScore, correctAnswer, idsToDraw);
        this.idsToDraw = idsToDraw;
    }

    @Override
    public void giveAccess(ThreeDGroupHandler threeDGroupHandler) {
        this.threeDGroupHandler = threeDGroupHandler;
        HashSet<String> allFileIDs = new HashSet<>(idsToDraw);

        for (String dir : this.resourceLocations) {
            this.threeDGroupHandler.getHasFXGroupContents().addResourceLocation(dir, allFileIDs);
        }
        for (File dir : this.fileDirs) {
            this.threeDGroupHandler.getHasFXGroupContents().addFileDir(dir, allFileIDs); //TODO
        }
    }

}
