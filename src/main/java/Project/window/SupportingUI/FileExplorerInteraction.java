package Project.window.SupportingUI;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class FileExplorerInteraction {

    /**
     * Browse file system to find save destination. gives warning if existing File is chosen.
     */
    public static File chooseSaveDestination(String dialogTitle, FileChooser.ExtensionFilter extensionFilter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(dialogTitle);
        fileChooser.setSelectedExtensionFilter(extensionFilter);
        return fileChooser.showSaveDialog(new Stage());
    }

    public static boolean writeToFile(String content, File file) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))){
            bufferedWriter.write(content);
            return true;
        } catch ( IOException e ) {
            System.out.println(e.getMessage());
            return false;
        }
    }


    public static File findDir(String dialogTitle) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle(dialogTitle);
        return dirChooser.showDialog(new Stage());
    }

    public static List<File> findTxt(String target) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Find " + target);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text file", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        return fileChooser.showOpenMultipleDialog(new Stage());
    }
}
