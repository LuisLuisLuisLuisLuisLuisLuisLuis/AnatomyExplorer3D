package Project.window.ThreeDPaneHandling.Effects;

import Project.SelectionModel.FXGroupDraw.HasFXGroupContentsContainer;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;


public class SelectionEffect {

    public enum Effect {
        VIA_SATURATION,
        VIA_DRAWMODE
    }

    /**
     * Applies any of this classes selection effect functions specified by the effect enum.
     */
    public static void applySelectionEffect(MeshView meshView, boolean selected, Effect effect) {
        if (effect == null) throw new NullPointerException("Effect cannot be null");
        if (effect == Effect.VIA_DRAWMODE) viaDrawmode(meshView, selected);
        else if (effect == Effect.VIA_SATURATION) viaSaturation(meshView, selected);
    }

    public static void transformSelectionMode(MeshView meshView, boolean isSelected, Effect from, Effect to) {
        if (from == Effect.VIA_DRAWMODE && to == Effect.VIA_SATURATION) transFormDrawModeToSaturationMode(meshView, isSelected, true);
        if (from == Effect.VIA_SATURATION && to == Effect.VIA_DRAWMODE) transFormDrawModeToSaturationMode(meshView, isSelected, false);
    }

    private static void transFormDrawModeToSaturationMode(MeshView meshView, boolean selected, boolean drawToSat) {
        if (drawToSat) {
            if (selected) {
                viaSaturation(meshView, true);
            } else {
                viaDrawmode(meshView, true);
            }
        } else {
            if (selected) {
                viaSaturation(meshView, false);
            } else {
                viaDrawmode(meshView, false);
            }
        }
    }

    /**
     * select -> DrawMode.FILL
     * unselect -> DrawMode.LINE
     */
    public static void viaDrawmode(MeshView meshView, boolean selected) {
        if (selected) meshView.setDrawMode(DrawMode.FILL);
        else meshView.setDrawMode(DrawMode.LINE);
    }

    /**
     * Meshview.Phongmaterial is desaturated twice for unselect and saturated twice (back to normal) for select.
     * Sets meshView.setMaterial(new PhongMaterial(Color.valueOf(HasFXGroupContentsContainer.DEFAULT_COLOR)) if MeshView.getMaterial() not instance of Phongmaterial
     */
    public static void viaSaturation(MeshView meshView, boolean selected) {
        if (!(meshView.getMaterial() instanceof PhongMaterial)) meshView.setMaterial(new PhongMaterial(Color.web(HasFXGroupContentsContainer.DEFAULT_COLOR_VAL)));

        PhongMaterial currentMaterial = (PhongMaterial) meshView.getMaterial();
        //things ive tried:
        //currentMaterial.setSpecularColor(Color.WHITE); //or other colors
        //currentMaterial.setSpecularPower(1200);
        //currentMaterial.getDiffuseColor().desaturate().desaturate().desaturate();

        if (selected) {
            currentMaterial.setDiffuseColor(currentMaterial.getDiffuseColor().saturate().saturate());
        } else {
            currentMaterial.setDiffuseColor(currentMaterial.getDiffuseColor().desaturate().desaturate());
        }  //works!
    }


    public static void viaColor(MeshView meshView, boolean selected, Color defaultColor) {
        Color color = Color.CRIMSON;
        if (meshView.getMaterial() instanceof PhongMaterial material) {
            if (selected) material.setDiffuseColor(color);
            else material.setDiffuseColor(defaultColor);
        }
    }

}
