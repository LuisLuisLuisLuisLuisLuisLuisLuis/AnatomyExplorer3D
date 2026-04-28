package Project.Util;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Utils {

    /**
     * Randomizes the order of the list.
     * @param l list to be shuffled
     */
    public static <T> void shuffleList(List<T> l) {
        Random r = new Random();
        l.sort(Comparator.comparingInt(a -> r.nextInt()));
    }
}
