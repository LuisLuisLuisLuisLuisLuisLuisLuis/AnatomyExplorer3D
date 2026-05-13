package Project.window.SupportingUI.PopUp;

import Project.model.ANode;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.HashMap;

public class InfoChart {
    /**
     * Makes an informative piechart.
     *
     * @param treeView
     * @return The piechart
     */
    public static Parent makePie(TreeView<ANode> treeView) {

        HashMap<String, Integer> parentDistribution = new HashMap<>();
        // Count direct parents of selected leaf nodes.
        for (TreeItem<ANode> treeItem : treeView.getSelectionModel().getSelectedItems()) {
            if (treeItem.getChildren().isEmpty()) {
                parentDistribution.put(
                        treeItem.getParent().getValue().name(),
                        parentDistribution.getOrDefault(treeItem.getParent().getValue().name(), 0) + 1
                );
            }
        }

        if (!parentDistribution.isEmpty()) {    // make a pie chart.
            ObservableList<PieChart.Data> piechartData = FXCollections.observableArrayList();
            for (String parentName : parentDistribution.keySet()) {
                piechartData.add(new PieChart.Data(parentName + " (" + parentDistribution.get(parentName) + ")", parentDistribution.get(parentName)));
            }

            PieChart pieChart = new PieChart(piechartData);
            pieChart.setTitle("Distribution of direct Parent Nodes of selected Leaf nodes");
            return pieChart;
        } else {
            VBox vBox = new VBox(new Text("No leaves selected!"));
            vBox.setPadding(new Insets(10));
            return vBox;
        }
    }
}
