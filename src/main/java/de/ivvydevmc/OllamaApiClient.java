package de.ivvydevmc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;

public class OllamaApiClient {
    //private static final Logger LOGGER = LoggerFactory.getLogger("spitball");

    // Inner class to hold both count and timestamp
    private static class RateLimitEntry {
        int count;
        long timestamp;

        RateLimitEntry(int count, long timestamp) {
            this.count = count;
            this.timestamp = timestamp;
        }
    }

    private Map<UUID, RateLimitEntry> rateLimiter = new HashMap<>();

    public OllamaApiClient() {
        //LOGGER.info("Ollama API Client created!");
    }

    private boolean isRateLimited(UUID playerUUID) {
        RateLimitEntry entry = rateLimiter.get(playerUUID);
        if (entry == null) return false;
        boolean limited = entry.count >= ConfigLoader.getMaxRequestsPerMinute();
        //LOGGER.info("Rate Limit for UUID: {} checked.", playerUUID);
        //LOGGER.info("Player limit status: {}", limited);
        return limited;
    }

    public CompletableFuture<String> getAnswerAsync(CommandContext<ServerCommandSource> source) {
        long now = System.currentTimeMillis();
        rateLimiter.entrySet().removeIf(entry -> now - entry.getValue().timestamp > 60000);

        UUID uuid = Objects.requireNonNull(source.getSource().getEntity()).getUuid();
        boolean isNew = false;
        RateLimitEntry entry = rateLimiter.get(uuid);
        if (entry == null) {
            entry = new RateLimitEntry(0, now);
            rateLimiter.put(uuid, entry);
            isNew = true;
        }
        if (isRateLimited(uuid) && !isNew) {
            if(source.getSource().getPlayer().hasPermissionLevel(4) && ConfigLoader.isOpsBypassLimit()) {
                //Nothing
            }
            else {return CompletableFuture.completedFuture("You have reached your maximum of " + ConfigLoader.getMaxRequestsPerMinute() + " request(s) per minute. Please try again later.");}
        }
        entry.count++;

        String fullPrompt = generateFullPrompt(ConfigLoader.getAdditionalInfo(), StringArgumentType.getString(source, "prompt"), ConfigLoader.getChatFormatting());

        // Run API call asynchronously and return the future
        return getAPIModelAnswerAsync(fullPrompt, ConfigLoader.getSystemPrompt(), ConfigLoader.getOllamaModel());
    }



    private String generateFullPrompt(String additionalInfo, String userPrompt, boolean useChatFormatting) {
        String chatFormattingText = "";
        if(useChatFormatting) {
            chatFormattingText = "You may also use chat formatting in the following way to highlight things were appropriate:" +
                                "Color codes are: §0–§f (black, dark blue, dark green, dark aqua, dark red, dark purple, " +
                                "gold, gray, dark gray, blue, green, aqua, red, light purple, yellow, white). Formatting styles are: " +
                                "§k (obfuscated), §l (bold), §m (strikethrough), §n (underlined), §o (italic), and §r (reset)." +
                                "Note that a color code resets previous formatting, so apply colors before styles";
        }
        return "Besides your system prompt, the owner of the server you are operating on has chosen to provide you with the following information: "
                + additionalInfo + "\n    The users inquiry is as follows. You are to answer in whatever language the user is using. If you believe " +
                "that the inquiry exceeds the confines of your role, please inform the user in an appropriate manner. Keep your answer concise and to the point: "
                + userPrompt + " Be aware, that your entire answer will be shown to the user, so you must only answer specifically to the prompt and not mention anything else."
                + chatFormattingText;
    }

    private static int estimateTokens(String text) {
        return (int) (text.split("\\s+").length * 1.5);  // Very generous estimate to accommodate other languages & punctuation
    }


    private CompletableFuture<String> getAPIModelAnswerAsync(String fullPrompt, String systemPrompt, String modelIdentifier) {
        return CompletableFuture.supplyAsync(() -> {
            //LOGGER.info("Getting model answer");
            String url = "http://" + ConfigLoader.getOllamaHostURL() + "/api/generate";

            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", modelIdentifier);
                requestBody.put("prompt", fullPrompt);
                requestBody.put("system", systemPrompt);

                JSONObject options = new JSONObject();
                options.put("num_ctx", (estimateTokens(fullPrompt) + estimateTokens(systemPrompt + 2048)));

                requestBody.put("options", options);
                requestBody.put("stream", false);

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))  // Set a connection timeout
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(60))  // Set a timeout for the request
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                //LOGGER.info(response.body().toString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("API request failed with status: " + response.statusCode());
                }

                JSONObject jsonResponse = new JSONObject(response.body());
                String responseText = jsonResponse.getString("response");
                if(responseText.isBlank()) {
                    throw new Exception();
                }
                return ConfigLoader.getResponsePrefix() + ": " + responseText;

            } catch (HttpTimeoutException e) {
                //LOGGER.error("API request timed out", e);
                return "Error: The request timed out.";
            } catch (Exception e) {
                //LOGGER.error("Failed to get API response", e);
                return "Error: Unable to fetch response from the AI model.";
            }
        });
    }
}