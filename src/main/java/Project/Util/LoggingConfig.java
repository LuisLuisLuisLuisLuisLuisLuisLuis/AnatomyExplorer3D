package Project.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.*;

public class LoggingConfig {

    public static void init() {init(Level.FINE, Level.CONFIG);}
    public static void init(Level level) {init(level, level);}
    /**
     * Setup root Logger.
     */
    public static void init(Level fileLevel, Level consoleLevel) {
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) handler.setLevel(consoleLevel);
        try {
            Path path = Paths.get(System.getProperty("java.io.tmpdir"), "anatomyViewer.log");
            FileHandler fileHandler = new FileHandler(path.toString(), 1_000_000, 3, true);
            fileHandler.setFormatter(new SimpleFormatter());
            root.addHandler(fileHandler);
            root.setLevel(fileLevel);

        } catch (IOException e) {
            root.addHandler(new ConsoleHandler());
        }
    }
}