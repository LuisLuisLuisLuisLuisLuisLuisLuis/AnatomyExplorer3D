package Project.command;

public interface Command {
    void undo();
    void execute();
    void redo();
    String name();

}
