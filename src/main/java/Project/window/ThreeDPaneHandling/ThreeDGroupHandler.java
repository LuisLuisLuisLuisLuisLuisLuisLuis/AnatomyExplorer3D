package Project.window.ThreeDPaneHandling;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.FXGroupDraw.HasFXGroupContentsContainer;
import Project.SelectionModel.FXGroupSelection.HasFXGroupSelection;
import Project.command.DrawCommands.ColorMeshviewsCommand;
import Project.command.DrawCommands.ResetViewDrawCommand;
import Project.window.ThreeDPaneHandling.Effects.SelectionEffect;
import Project.window.ThreeDPaneHandling.Movement.CamMover;
import Project.window.ThreeDPaneHandling.Movement.Group3DRotation;
import Project.window.ThreeDPaneHandling.Movement.MouseScrolling3D;
import Project.window.ThreeDPaneHandling.Movement.SetupMouseRotate3D;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;

import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Logger;

import static Project.window.WindowPresenter.centerGroupToItself;

public class ThreeDGroupHandler {

    private static final Logger logger = Logger.getLogger(ThreeDGroupHandler.class.getName());

    private final String handlerID;

    private final Pane pane;
    private final PerspectiveCamera perspectiveCamera;//  = new PerspectiveCamera(true); //camera in 3D view
    public static Point3D DEFAULT_INIAL_CAMERA_POSITION = new Point3D(0,-10,-1500);

    private static final Affine INITIAL_TRANSFORM = new Affine( //initial transform of the group holding the 3D contents. serves as reset position
            1.0, 0.0, 0.0, 0.0,
            0.0, 0.0, -1.0, 0.0,
            0.0, 1.0, 0.0, 0.0
    );

    // these two doubles decide how large each step of zoom (via scrolling) or rotation is
    private static final double zoomStep = 4;
    private static final double rotationStep = 2;

    private final Group root3d;         //root group of the 3d view holding the groups below.
    private final Group slicePlaneGroup;//will hold plane for slicing meshes
    private final Group contentGroup;   //holds the innerGroup
    private final Group innerGroup;     //holds the meshViews

    private final HasFXGroupContents hasFXGroupContents;
    private final HasFXGroupContentsContainer hasFXGroupContentsContainer;
    private final HasFXGroupSelection hasFXGroupSelection;

    private boolean clickSelectable = true; // toggle if the user can select meshviews via mouse click. enabled by default

    private final CamMover camMover; // controls camera movement
    private final Group3DRotation contentGroupRotator;      // implements rotation of the 3D view via rotation of a group
    private final Group3DRotation slicePlaneGroupRotator;   // rotates the slicing plane
    private final SetupMouseRotate3D setupMouseRotate3D;    // uses groupRotater to rotate a group using mouse drag
    private boolean isRotating = false; //is the user rotating the 3D view via mouse drag right now? if so, disable click-selecting meshViews

    // the currently chosen selection effect
    private SelectionEffect.Effect selectionEffect = SelectionEffect.Effect.VIA_SATURATION;
    public SelectionEffect.Effect getSelectionEffect() {return this.selectionEffect;}
    public void setSelectionEffect(SelectionEffect.Effect selectionEffect) {this.selectionEffect = selectionEffect;}

    public HasFXGroupContents getHasFXGroupContents() {return this.hasFXGroupContents;}
    public HasFXGroupContentsContainer getHasFXGroupContentsContainer () {return this.hasFXGroupContentsContainer;}
    public HasFXGroupSelection getHasFXGroupSelection() {return this.hasFXGroupSelection;}


    public ThreeDGroupHandler(Pane pane, PerspectiveCamera camera, Point3D initialCameraPosition, String id) {
        this.handlerID = id;
        this.pane = pane;
        this.perspectiveCamera = camera;
        this.camMover = new CamMover(perspectiveCamera);

        //initialize the contentGroup via Setup3DSubPane
        LinkedList<Group> outerGroups = Setup3DSubPane.setup3DSubPane(this.pane, camera, initialCameraPosition);
        root3d = outerGroups.get(0);    // root3d holds contentGroup.
        // If you want to add more content that is independet from the meshes (i.e. independent from innerGroup),
        // add a new group to root3d and put it in there. like for example the slice pane.
        // It will not be affected by clearing the view or any rotation, mouse drag etc unless you add that group to
        // specific methods (like SetupMouseRotate3D).
        slicePlaneGroup = new Group();
        root3d.getChildren().add(this.slicePlaneGroup);
        contentGroup = outerGroups.get(1);
        this.innerGroup = new Group();


        // Rotation
        this.contentGroupRotator = new Group3DRotation(contentGroup);
        this.slicePlaneGroupRotator = new Group3DRotation(slicePlaneGroup);
        setupMouseRotate3D = new SetupMouseRotate3D(this.pane, new Group[]{contentGroup, slicePlaneGroup});

        //mouse scrolling functionality
        MouseScrolling3D mouseScrolling3D = new MouseScrolling3D(new Group3DRotation[] {contentGroupRotator, slicePlaneGroupRotator},this.pane);


        contentGroup.getChildren().add(innerGroup);
        contentGroup.getTransforms().setAll(INITIAL_TRANSFORM);



        //--------------------------3D draw model-------------------
        this.hasFXGroupContents = new HasFXGroupContents(innerGroup, id + "hasFXContents");
        this.hasFXGroupContentsContainer = new HasFXGroupContentsContainer(hasFXGroupContents); //, new String[]{isAFilesLocation.getValue().getAbsolutePath(), partOfFilesLocation.getValue().getAbsolutePath()}
        this.hasFXGroupContents.setHasFXGroupContentsContainer(hasFXGroupContentsContainer);
        //---------------------------


        //---------------initialize 3D selection model-----------
        hasFXGroupSelection = new HasFXGroupSelection(hasFXGroupContents, id + "hasFXSelection");
        //-----------------------------------------------

        hasFXGroupContents.getSelection().addListener((InvalidationListener) e -> centerGroupToItself(innerGroup));  //should be called after the command below, otherwise it'll fire every loop

        //---------------implement 3D mouse selection------------------
        innerGroup.setOnMouseReleased(event -> {
            if (isRotating) {
                isRotating = false; //dont de-/select when you were just mouse dragging for rotating
                return;
            }
            if (clickSelectable) {
                PickResult result = event.getPickResult();
                Node pickedNode = result.getIntersectedNode();
                if (pickedNode instanceof MeshView mesh) {
                    if (!hasFXGroupSelection.getSelection().contains(mesh)) {
                        hasFXGroupSelection.select(mesh);
                    } else {
                        hasFXGroupSelection.unselect(mesh);
                    }
                }
            }
        });
        innerGroup.setOnMouseDragged(event -> isRotating = true); //user is currently rotating ;)
        //---------------------------------------------

        //------------Call change of appearance of MeshViews upon un-/selecting them------------
        hasFXGroupSelection.getSelection().addListener((ListChangeListener<MeshView>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (MeshView node : c.getAddedSubList()) {
                        if (node != null) {
                            meshViewSelectionEffect(node, true);

                        }
                    }
                }
                if (c.wasRemoved()) {
                    for (MeshView node : c.getRemoved()) {
                        if (node != null) {
                            meshViewSelectionEffect(node, false);
                        }
                    }
                }
            }
        });
        //---------------------------------------------------

    }

    /**
     * Apply the 3D selection effect.
     * @param meshView to select.
     * @param selected: to select or to unselect (decides which effect is applied).
     */
    private void meshViewSelectionEffect(MeshView meshView, boolean selected) {
        SelectionEffect.applySelectionEffect(meshView, selected, selectionEffect);
    }

    /**
     * Change the selectionEffect of this to the newEffect. Then apply the new effect on all MeshViews of threeDContentGroup.
     * @param newEffect the new effect
     */
    public void changeSelectionEffect(SelectionEffect.Effect newEffect) {
        for (MeshView meshView : hasFXGroupContents.getAllItems()) {
            boolean isSelected = hasFXGroupSelection.getSelection().contains(meshView);
            SelectionEffect.transformSelectionMode(meshView, isSelected, this.selectionEffect, newEffect);
        }
        this.selectionEffect = newEffect;
    }

    public void resetView() {
        new ResetViewDrawCommand(new Group[]{contentGroup},perspectiveCamera, INITIAL_TRANSFORM, DEFAULT_INIAL_CAMERA_POSITION, setupMouseRotate3D).execute();
    }

    public void resetColors() {
        for (MeshView meshView : getHasFXGroupContents().getAllItems()) {
            new ColorMeshviewsCommand(hasFXGroupContentsContainer.getDefaultColorForID(meshView.getId()), Set.of(meshView.getId()), hasFXGroupContents, hasFXGroupSelection).execute();
        }
    }

    /**
     * Resets modifications to the MeshViews back to their default values.
     * <ul>
     *     <li>Resets colors</li>
     *     <li>Resets perspective</li>
     * </ul>
     * Does NOT change which MeshViews are drawn.
     */
    public void resetModifiables() {
        resetView();
        resetColors();
    }

    /**
     * @return can the user select MeshViews via mouse click?
     */
    public boolean isClickSelectable() {return clickSelectable;}

    /**
     * @param clickSelectable true -> the user can select MeshViews via mouse click. false -> they cannot.
     */
    public void setClickSelectable(boolean clickSelectable) {this.clickSelectable = clickSelectable;}

}
