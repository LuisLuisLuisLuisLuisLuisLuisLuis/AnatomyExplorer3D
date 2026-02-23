package Project.window.PopUp;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;


public class Help {

    public static WebView getHelp() {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        URL doc = Help.class.getResource("/Project/Doc.html");
        if (doc != null) webEngine.load(doc.toExternalForm());
        else System.out.println("Doc is null!");
        return webView;
    }

}
