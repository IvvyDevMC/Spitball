package de.ivvydevmc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpitballLoader implements ModInitializer {

    public static final String MOD_ID = "spitball";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final OllamaApiClient client = new OllamaApiClient();

    private static void getAnswerAsync(CommandContext<ServerCommandSource> context) {
        //LOGGER.info("Obtaining answer from Ollama client!");
        //LOGGER.info("Server URL is: " + ConfigLoader.getOllamaHostURL());

        client.getAnswerAsync(context).thenAccept(answer -> {
            context.getSource().sendFeedback(() -> Text.literal(answer), false);
        });
    }


    private static int run(CommandContext<ServerCommandSource> commandContext) {
        //LOGGER.info("Run Method in spitball main class triggered");

        // Call the async method
        getAnswerAsync(commandContext);

        // Return immediately since the actual response will be sent asynchronously
        return 1;
    }


    @Override
    public void onInitialize() {
        ConfigLoader.loadConfig();

        LOGGER.info("Hello from " + MOD_ID);


        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, regEnvironment) -> dispatcher.register(CommandManager.literal(ConfigLoader.getChatCommand())
                .requires(source -> source.hasPermissionLevel(ConfigLoader.getPermissionLevel()))
                .then(CommandManager.argument("prompt", StringArgumentType.string())
                .executes(SpitballLoader::run)))));

    }
}
