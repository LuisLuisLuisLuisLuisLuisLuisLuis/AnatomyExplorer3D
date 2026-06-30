package Project.AI;


import Project.AI.Format.OpenAIRequest;
import Project.AI.Format.OpenAIResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;

/**
 * Posts a request to API. You need a key to use this.
 */
public class AI_API_Task extends Task<String> {

    private final OpenAIRequest openAIRequest;
    private final String key;

    private final String api_url = ".../chat/completions";

    /**
     * @param openAIRequest: Your request.
     * @param key: Your key.
     */
    public AI_API_Task(OpenAIRequest openAIRequest, String key) {
        this.key = key;
        this.openAIRequest = openAIRequest;
    }

    @Override
    protected String call() {

        if (false) { //enable testing without calling API
            System.out.println("giving mock answer");
            return "(aiMockAnswer|clavicle|sternum|rib)" + "(aiMockAnswer|clavicle|sternum|rib)" + "(aiMockAnswer|clavicle|sternum|rib)";
        }

        ObjectMapper objectMapper = new ObjectMapper(); // json parsing
        String requestString;
        try {
            requestString = objectMapper.writeValueAsString(this.openAIRequest); //parse messages to string
        } catch (JsonProcessingException e) {
            System.out.println(e.getMessage());
            return e.getMessage();
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()  // build request format
                .uri(URI.create(api_url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + key)
                .POST(HttpRequest.BodyPublishers.ofString(requestString))
                .build();

        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            System.out.println("HTTP send gone wrong");
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            System.out.println("HTTP send gone wrong");
            throw new RuntimeException(e);
        }

        if (httpResponse.statusCode() != 200) {
            System.out.println("httpResponse statuscode: " + httpResponse.statusCode());
            throw new RuntimeException("API error: " + httpResponse.body());
        }


        OpenAIResponse chatResponse;
        try {
            chatResponse = objectMapper.readValue(httpResponse.body(), OpenAIResponse.class);   // write API response to objects. got this idea from my good friend gpt-4o
        } catch (JsonMappingException e) {
            System.err.println(e.getMessage());
            return e.getMessage();
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
            return e.getMessage();
        }

        return chatResponse.choices.get(0).message.content;
    }

}
