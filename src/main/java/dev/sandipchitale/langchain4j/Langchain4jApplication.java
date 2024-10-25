package dev.sandipchitale.langchain4j;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.github.GitHubModelsChatModel;
import dev.langchain4j.model.github.GitHubModelsStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.model.github.GitHubModelsChatModelName.GPT_4_O_MINI;


@SpringBootApplication
public class Langchain4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(Langchain4jApplication.class, args);
    }

    private static class LowerCaser {
        @Tool("Lowercase the string")
        String lowerCase(String stringToLowercase) {
            return stringToLowercase.toLowerCase();
        }
    }


    static interface Assistant {
        String chat(String chatMessage);
    }

    @Bean
    @Order(10)
    public CommandLineRunner nonStreaming() {
        return (String... args) -> {
            System.out.println("All response at once i.e. non-streaming....");
            GitHubModelsChatModel model = GitHubModelsChatModel.builder()
                    .gitHubToken(System.getenv("GITHUB_TOKEN"))
                    .modelName(GPT_4_O_MINI)
                    .logRequestsAndResponses(true)
                    .build();

            String response = model.generate("Provide 10 short numbered bullet points in plain text, no markdown, explaining why Java is awesome");

            System.out.println(response);
            System.out.println("-".repeat(80));
        };
    }

    @Bean
    @Order(20)
    public CommandLineRunner streaming() {
        return (String... args) -> {
            System.out.println("Streaming....");
            GitHubModelsStreamingChatModel model = GitHubModelsStreamingChatModel.builder()
                    .gitHubToken(System.getenv("GITHUB_TOKEN"))
                    .modelName(GPT_4_O_MINI)
                    .logRequestsAndResponses(true)
                    .build();

            String userMessage = "Provide 10 short numbered bullet points in plain text, no markdown, explaining why Java is awesome";

            CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();
            model.generate(userMessage, new StreamingResponseHandler<AiMessage>() {

                @Override
                public void onNext(String token) {
                    System.out.print(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    System.out.println();
                    System.out.println("-".repeat(80));
                    futureResponse.complete(response);
                }

                @Override
                public void onError(Throwable error) {
                    futureResponse.completeExceptionally(error);
                }
            });

            futureResponse.join();
        };
    }

    @Bean
    @Order(30)
    public CommandLineRunner toolUse() {
        return (String... args) -> {
            System.out.println("Using tool....");
            GitHubModelsChatModel model = GitHubModelsChatModel.builder()
                    .gitHubToken(System.getenv("GITHUB_TOKEN"))
                    .modelName(GPT_4_O_MINI)
                    .logRequestsAndResponses(true)
                    .build();

            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .tools(new LowerCaser())
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .build();

            String response = assistant
                    .chat("Shout something in uppercase and then lowercase that string");
            System.out.println(response);
            System.out.println("-".repeat(80));
        };
    }

}
