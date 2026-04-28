package Project.window.Quiz;

public class Difficulty {
    private final int difficulty;
    private final String name;

    public Difficulty(int difficulty, String name) {
        this.difficulty = difficulty;
        this.name = name;
    }

    public int getDifficultyLevel() {return difficulty;}

    @Override
    public String toString() {return name;}

    public static final Difficulty VERY_EASY = new Difficulty(100, "VERY EASY");
    public static final Difficulty EASY = new Difficulty(200, "EASY");
    public static final Difficulty MEDIUM = new Difficulty(300, "MEDIUM");
    public static final Difficulty HARD = new Difficulty(400, "HARD");
    public static final Difficulty VERY_HARD = new Difficulty(500, "VERY HARD");
}
