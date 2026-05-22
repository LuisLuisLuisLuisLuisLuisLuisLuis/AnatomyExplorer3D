package Project.window.SupportingUI.PopUp;

import com.dlsc.pdfviewfx.PDFView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Help {

    private static final Logger logger = Logger.getLogger(Help.class.getName());

    public static WebView getHelp() {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        URL doc = Help.class.getResource("/Project/doc/guide/HTML/main.html");
        if (doc != null) webEngine.load(doc.toExternalForm());
        else logger.log(Level.SEVERE, "Help doc is null");
        return webView;
    }

    public static void showGuide() {
        PDFView pdfView = new PDFView();    //doesnt get any easier <3
        pdfView.load(Help.class.getResourceAsStream("/Project/guide.pdf"));
        LittlePopUp.showPopup(pdfView, "Guide", 1000, 500);

    }

}
