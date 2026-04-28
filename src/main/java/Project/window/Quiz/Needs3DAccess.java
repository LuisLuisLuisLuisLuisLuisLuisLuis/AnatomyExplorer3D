package Project.window.Quiz;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContents;
import Project.SelectionModel.FXGroupDraw.HasFXGroupContentsContainer;
import Project.SelectionModel.FXGroupSelection.HasFXGroupSelection;
import Project.window.ThreeDPaneHandling.ThreeDGroupHandler;

/**
 * A Question that needs to draw something in 3D using HasFXGroupContents and HasFXGroupSelection.
 * First, load all necessary files given by getMeshViewsToLoad() and then enable this Question to draw by giveAccess().
 * Enable drawing by giveAccess().
 */
public interface Needs3DAccess {
    /**
     * Enable this question to draw and select 3D components.
     * @param threeDGroupHandler a ThreeDGroupHandler
     */
    void giveAccess(ThreeDGroupHandler threeDGroupHandler);

}
