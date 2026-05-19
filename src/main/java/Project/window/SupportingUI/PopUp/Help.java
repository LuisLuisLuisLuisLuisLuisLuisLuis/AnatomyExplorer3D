package Project.window.SupportingUI.PopUp;

import com.sun.net.httpserver.HttpServer;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Help {

    private static final Logger logger = Logger.getLogger(Help.class.getName());

    public static WebView getHelp() {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        URL doc = Help.class.getResource("/Project/Doc.html");
        if (doc != null) webEngine.load(doc.toExternalForm());
        else logger.log(Level.SEVERE, "Help doc is null");
        return webView;
    }

    public static WebView getGuide() {

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setUserStyleSheetLocation(Help.class.getResource("/Project/Styles/pdfEngine.css").toExternalForm()); //hide toolbar. dont want user to use pdf.js to load their own PDFs

        URL viewer = Help.class.getResource("/Project/pdfjs-2.16.105-legacy-dist/web/viewer.html");
        URL pdf = Help.class.getResource("/Project/cpmb.pdf");
        if (viewer == null || pdf == null) {
            logger.log(Level.SEVERE, "Failed to load guide because viewer.html of PDF.js or the guide.pdf resource could not be resolved.");
            return webView;
        }
        /*
         * Approach adapted from:
         * Source - https://stackoverflow.com/a/42040344
         * Posted by enzo
         * Retrieved 2026-05-19, License - CC BY-SA 3.0
         */
        engine.getLoadWorker().stateProperty().addListener((obs, old, nw) -> {
            // this pdf file will be opened on application startup
            if (nw == Worker.State.SUCCEEDED) {
                try {
                    byte[] data = pdf.openStream().readAllBytes();
                    String base64 = Base64.getEncoder().encodeToString(data);
                    // call JS function from Java code
                    engine.executeScript("openFileFromBase64('" + base64 + "')");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to load guide.", e);
                }
            }
        });

        engine.load(viewer.toExternalForm());

        return webView;
    }

}
