package Project.window.SupportingUI.PopUp;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;

public class About {
    public static WebView getAbout() {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        URL doc = Help.class.getResource("/Project/About.html");
        if (doc != null) webEngine.load(doc.toExternalForm());
        else System.out.println("About is null!");
        return webView;
    }
}
