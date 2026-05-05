package Project.window.Quiz.Question;

import Project.command.DrawCommands.ColorMeshviewsCommand;
import Project.window.Quiz.Difficulty;
import Project.window.Slicing.Plane;
import Project.window.ThreeDPaneHandling.Objects.Arrow;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.paint.Color;

import java.net.URI;
import java.util.List;
import java.util.Set;

public class SimpleSliceMCQuestion<T,S extends Comparable<S>> extends SimpleMultipleChoice3DQuestion<T, S> {

    /**
     * MeshView FileID, NOT conceptID!
     */
    private String sliceTarget = null;

    /**
     * @param sliceTargetID ID of a MeshView that will be used to position the slice plane in such a way that it slices the MeshView in half.
     */
    public void setSliceTarget(String sliceTargetID) {
        this.sliceTarget = sliceTargetID;
    }

    private Point3D rotateAxis = null;
    private double rotateAngle = 0;

    /**
     * For now, this question can only slice in the planes of the body axes because that's what exams show.
     * ***
     * Give an axis and angle by which the slice plane is to be rotated before slicing.
     * @param axis axis
     * @param angle angle
     */
    private void setRotateSlicePlane(Point3D axis, double angle) {
        this.rotateAxis = axis;
        this.rotateAngle = angle;
    }

    public void setSliceAxis(Plane.BodyAxis bodyAxis, boolean sliceAxisPositive) {
        this.sliceAxis = bodyAxis;
        this.sliceAxisPositive = sliceAxisPositive;
    }

    protected Plane.BodyAxis sliceAxis = Plane.BodyAxis.AXIAL;  //default axial looking slice looking downward
    protected boolean sliceAxisPositive = false;

    /**
     * A simple multiple choice question model with only one correct answer. Draws some stuff in 3D and asks via multiple choice.
     * Only one correct answer.
     *
     * @param difficulty        difficulty of the question
     * @param minScore          minimum score
     * @param maxScore          max score
     * @param correctAnswer     correct answer
     * @param possibleAnswers   choices this question. it is only possible to answer with one of the choices
     * @param idsToDraw         Meshview IDs that will be drawn upon asking the question. In the format of filenames of OBJ files.
     * @param idsToSelect       Meshviews that will be selected upon asking the question. In the format of filenames of OBJ files.
     * @param resourceLocations where to find the OBJ files specified by idsToDraw and idsToSelect
     */
    public SimpleSliceMCQuestion(Difficulty difficulty, S minScore, S maxScore, T correctAnswer, List<T> possibleAnswers, List<String> idsToDraw, List<String> idsToSelect, List<URI> resourceLocations) {
        super(difficulty, minScore, maxScore, correctAnswer, possibleAnswers, idsToDraw, idsToSelect, resourceLocations);
    }

    /** between 0 and 5 incl.*/
    protected int arrowConfigCount = 0;
    protected boolean arrowAxisPositive = sliceAxisPositive;

    protected void changeArrow() {
        threeDGroupHandler.removeArrow(arrow);
        boolean dontPierce = !(arrowConfigCount == 0);
        int arrowPlaneInd = arrowConfigCount%2;
        Plane.BodyAxis arrowPlane = getOrthogonalPlanes(sliceAxis)[arrowPlaneInd];

        arrow = threeDGroupHandler.pointArrowAt(threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(sliceTarget), arrowPlane, arrowAxisPositive, dontPierce, arrowColor);

        if (arrowPlaneInd==0) arrowAxisPositive = !arrowAxisPositive;

        if (arrowConfigCount == 5) arrowConfigCount = 0; else arrowConfigCount++;

    }

    protected Arrow arrow = null;
    protected Color arrowColor = Color.RED;

    /**
     * Return an array of the two possible planes to put the arrow in given the plane of the slice.
     * @param sliceAxis slice plane
     * @return array of size 2
     */
    protected static Plane.BodyAxis[] getOrthogonalPlanes(Plane.BodyAxis sliceAxis) {
        return switch (sliceAxis) {
            case AXIAL -> new Plane.BodyAxis[]{Plane.BodyAxis.SAGGITAL, Plane.BodyAxis.CORONAL};
            case SAGGITAL -> new Plane.BodyAxis[]{Plane.BodyAxis.CORONAL, Plane.BodyAxis.AXIAL};
            case CORONAL -> new Plane.BodyAxis[]{Plane.BodyAxis.AXIAL, Plane.BodyAxis.SAGGITAL};
        };
    }


    @Override
    public Node ask() {
        if (sliceTarget == null) {
            Label l = new Label("Slice target not provided. Cannot ask question.");
            l.setWrapText(true);
            this.formattedQuestionUI.getChildren().clear();
            this.formattedQuestionUI.setTop(l);
            submit(correctAnswer);
            // at this point, there is no submit button anymore -> user cannot work on this question.
            return formattedQuestionUI;
        }
        Node node = super.ask();

        if (!nonExist.isEmpty()) return node;   // if something could not be loaded, return.

        Button resetViewButton = new Button("Reset View");
        resetViewButton.setOnAction(e -> focusTarget());
        Button changeArrowButton = new Button("Change Arrow");
        changeArrowButton.setOnAction(e -> changeArrow());
        formattedQuestionUI.setBottom(new ToolBar(resetViewButton, changeArrowButton));

        if (threeDGroupHandler.getHasFXGroupContents().getSelection().isEmpty()) return node;

        threeDGroupHandler.createSlicePlaneAtMeshView(sliceTarget, sliceAxis, sliceAxisPositive);

//        this.arrow = threeDGroupHandler.pointArrowAt(
//                threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(sliceTarget),
//                getArrowPlaneForSlicePlane(sliceAxis),
//                sliceAxisPositive,
//                false,
//                arrowColor);
        changeArrow();

        threeDGroupHandler.slice(threeDGroupHandler.getHasFXGroupContents().getSelection());    //slice everything

        focusTarget();

        threeDGroupHandler.getHasFXGroupSelection().select(threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(sliceTarget));
        new ColorMeshviewsCommand(arrowColor, Set.of(sliceTarget), threeDGroupHandler.getHasFXGroupContents(), threeDGroupHandler.getHasFXGroupSelection()).execute();

        return node;
    }

    /**
     * Shifts the view so that the slice target is approximately in the middle.
     */
    protected void focusTarget() {
        threeDGroupHandler.resetView();
        switch (sliceAxis) {
            case SAGGITAL -> {threeDGroupHandler.applyGlobalRotation(new Point3D(0,1,0), (sliceAxisPositive ? 270 : 90));}
            case AXIAL -> {threeDGroupHandler.applyGlobalRotation(new Point3D(1,0,0), (sliceAxisPositive ? 270 : 90));}
            case CORONAL -> {threeDGroupHandler.applyGlobalRotation(new Point3D(0,1,0), (sliceAxisPositive ? 180 : 0));}
        }
        threeDGroupHandler.viewToMeshView(threeDGroupHandler.getHasFXGroupContents().getMeshViewWithID(sliceTarget));
    }
}
