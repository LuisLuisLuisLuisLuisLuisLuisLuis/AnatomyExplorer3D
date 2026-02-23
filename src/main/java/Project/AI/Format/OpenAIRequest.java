package Project.AI.Format;

import java.util.List;

/**
 * A request with fields required by OpenAI API.
 */
public class OpenAIRequest {
    public String model = "gpt-3.5-turbo";
    public List<OpenAIMessage> messages;

    public OpenAIRequest(List<OpenAIMessage> messages) {
        this.messages = messages;
    }
    public OpenAIRequest(List<OpenAIMessage> messages, String model) {this.messages = messages; this.model = model;}
}
