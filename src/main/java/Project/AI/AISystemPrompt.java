package Project.AI;

public class AISystemPrompt {
    public static String prompt1 = """
            You are an expert in anatomy and Java regular expressions.
            You have access to the following list of terms:

            ---INSERT TREE---

            Given a user request, please provide a regex that matches to all terms
            that fall under the anatomical interpretation of the user request. Only respond
            with the Java regex, no explanation. Thank you.
            """;
}
