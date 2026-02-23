package Project.AI;

import Project.AI.Format.OpenAIMessage;
import Project.AI.Format.OpenAIRequest;
import javafx.concurrent.Service;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides AI service via a Task.
 */

public class AIService extends Service<String> {

    private String model = "gpt-3.5-turbo";
    public void setModel(String model) {this.model = model;}

    private final String key;
    private final List<OpenAIMessage> messages = new LinkedList<>();

    /**
     * You need a key to create this Service.
     * @param key as String.
     */
    public AIService(String key) {
        this.key = key;
    }

    /**
     * Clears the chat history and initializes with a new system prompt.
     * @param systemPrompt as String.
     */
    public void initializeChat(String systemPrompt) {
        messages.clear();
        messages.add(new OpenAIMessage("system", systemPrompt));
    }

    /**
     * Add a user message.
     * @param message as String.
     */
    public void addMessage(OpenAIMessage message) {
        this.messages.add(message);
    }
    public void addQnA(QnA qna) {
        messages.add(new OpenAIMessage("user", qna.getQ()));
        messages.add(new OpenAIMessage("assistant", qna.getA()));
    }

    /**
     * @return the last message.
     */
    public OpenAIMessage getLastMessage() {
        return messages.getLast();
    }

    @Override
    protected AI_API_Task createTask() {
        return new AI_API_Task(new OpenAIRequest(messages, model), this.key);
    }

}
