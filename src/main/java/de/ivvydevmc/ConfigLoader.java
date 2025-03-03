package de.ivvydevmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {
    // Path to the configuration file
    private static final Path CONFIG_PATH = Paths.get("config", "spitball.toml");
    // Map to hold the loaded config values
    private static final Map<String, Object> configValues = new HashMap<>();
    // Default configuration values
    private static final Map<String, Object> defaultValues = new HashMap<>();

    static {
        defaultValues.put("permissionLevel", 0);
        defaultValues.put("broadcastToOps", false);
        defaultValues.put("ollamaHostURL", "");
        defaultValues.put("maxRequestsPerMinute", 1);
        defaultValues.put("opsBypassLimit", true);
        defaultValues.put("systemPrompt", "\"You are a helpful AI on a minecraft server\"");
        defaultValues.put("additionalInfo", "");
        defaultValues.put("ollamaModel", "");
        defaultValues.put("chatCommand", "\"guide\"");
        defaultValues.put("responsePrefix", "\"§c[§aGuide§c]§r\"");
        defaultValues.put("chatFormatting", false);
    }

    // Loads configuration from file, or writes defaults if missing
    public static void loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                //Logger LOGGER = LoggerFactory.getLogger("spitball");
                //LOGGER.info("Creating config file.");
                writeDefaultConfig();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Ignore empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) continue;
                int equalsIndex = line.indexOf("=");
                if (equalsIndex < 0) continue;
                String key = line.substring(0, equalsIndex).trim();
                String valuePart = line.substring(equalsIndex + 1).trim();
                Object value = parseValue(key, valuePart);
                configValues.put(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Validate permissionLevel range
        if (configValues.containsKey("permissionLevel")) {
            int level = (Integer) configValues.get("permissionLevel");
            if (level < 0 || level > 4) {
                System.out.println("permissionLevel out of range, resetting to default.");
                configValues.put("permissionLevel", defaultValues.get("permissionLevel"));
            }
        }
    }

    // Parses a value based on the key, supporting int, boolean, and string types
    private static Object parseValue(String key, String valuePart) {
        // For booleans
        switch (key) {
            case "broadcastToOps", "opsBypassLimit", "chatFormatting" -> {
                return Boolean.parseBoolean(valuePart);
            }

            // For integers
            case "permissionLevel", "maxRequestsPerMinute" -> {
                try {
                    return Integer.parseInt(valuePart);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid integer for key " + key + ". Using default.");
                    return defaultValues.get(key);
                }
            }

            // For strings (remove surrounding quotes if present)
            case "ollamaHostURL", "systemPrompt", "additionalInfo", "ollamaModel", "chatCommand", "responsePrefix" -> {
                if ((valuePart.startsWith("\"") && valuePart.endsWith("\"")) ||
                        (valuePart.startsWith("'") && valuePart.endsWith("'"))) {
                    valuePart = valuePart.substring(1, valuePart.length() - 1);
                }
                return valuePart;
            }
        }
        // Fallback: return the raw string
        return valuePart;
    }

    // Writes the default configuration file with comments
    private static void writeDefaultConfig() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
            writer.write("# Configuration for Spitball");
            writer.newLine();
            writer.newLine();

            writer.write("# Permission level (between 0 and 4). Default: 0");
            writer.newLine();
            writer.write("permissionLevel = " + defaultValues.get("permissionLevel"));
            writer.newLine();
            writer.newLine();

            writer.write("# Whether to broadcast messages to ops. Default: false");
            writer.newLine();
            writer.write("broadcastToOps = " + defaultValues.get("broadcastToOps"));
            writer.newLine();
            writer.newLine();

            writer.write("# The host URL for Ollama. Default: empty");
            writer.newLine();
            writer.write("ollamaHostURL = \"\"");  // Empty string as default
            writer.newLine();
            writer.newLine();

            writer.write("# Maximum requests per minute. Default: 1");
            writer.newLine();
            writer.write("maxRequestsPerMinute = " + defaultValues.get("maxRequestsPerMinute"));
            writer.newLine();
            writer.newLine();

            writer.write("# Whether ops bypass the request limit. Default: true");
            writer.newLine();
            writer.write("opsBypassLimit = " + defaultValues.get("opsBypassLimit"));
            writer.newLine();
            writer.newLine();

            writer.write("# System prompt. Default: You are a helpful AI on a minecraft server");
            writer.newLine();
            writer.write("systemPrompt = " + defaultValues.get("systemPrompt"));
            writer.newLine();
            writer.newLine();

            writer.write("# Additional info about your Server the AI should have. Default: empty");
            writer.newLine();
            writer.write("additionalInfo = \"\"");
            writer.newLine();
            writer.newLine();

            writer.write("# The model on your ollama instance to be used. Default: empty");
            writer.newLine();
            writer.write("ollamaModel = \"\"");
            writer.newLine();
            writer.newLine();

            writer.write("# The command used to access the AI. Default: \"guide\"");
            writer.newLine();
            writer.write("chatCommand = " + defaultValues.get("chatCommand"));
            writer.newLine();
            writer.newLine();

            writer.write("# The prefix AI responses have in your chat. Default: \"[Guide]\"");
            writer.newLine();
            writer.write("responsePrefix = " + defaultValues.get("responsePrefix"));
            writer.newLine();
            writer.newLine();

            writer.write("# Whether the AI should use chat formatting (Additional information sent to AI, so response times are increased). Default: false");
            writer.newLine();
            writer.write("chatFormatting = " + defaultValues.get("chatFormatting"));
            writer.newLine();
            writer.newLine();
        }
    }

    // Getters for accessing config values at runtime
    public static int getPermissionLevel() {
        return (int) configValues.getOrDefault("permissionLevel", defaultValues.get("permissionLevel"));
    }

    public static boolean isBroadcastToOps() {
        return (boolean) configValues.getOrDefault("broadcastToOps", defaultValues.get("broadcastToOps"));
    }

    public static String getOllamaHostURL() {
        return (String) configValues.getOrDefault("ollamaHostURL", defaultValues.get("ollamaHostURL"));
    }

    public static int getMaxRequestsPerMinute() {
        return (int) configValues.getOrDefault("maxRequestsPerMinute", defaultValues.get("maxRequestsPerMinute"));
    }

    public static boolean isOpsBypassLimit() {
        return (boolean) configValues.getOrDefault("opsBypassLimit", defaultValues.get("opsBypassLimit"));
    }

    public static String getSystemPrompt() {
        return (String) configValues.getOrDefault("systemPrompt", defaultValues.get("systemPrompt"));
    }

    public static String getAdditionalInfo() {
        return (String) configValues.getOrDefault("additionalInfo", defaultValues.get("additionalInfo"));
    }

    public static String getOllamaModel() {
        return (String) configValues.getOrDefault("ollamaModel", defaultValues.get("ollamaModel"));
    }

    public static String getChatCommand() {
        return (String) configValues.getOrDefault("chatCommand", defaultValues.get("chatCommand"));
    }

    public static String getResponsePrefix() {
        return (String) configValues.getOrDefault("responsePrefix", defaultValues.get("responsePrefix"));
    }

    public static boolean getChatFormatting() {
        return (boolean) configValues.getOrDefault("chatFormatting", defaultValues.get("chatFormatting"));
    }
}
