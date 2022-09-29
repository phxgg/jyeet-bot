package bot;

import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.dto.Response;
import bot.dto.Server;
import com.google.gson.Gson;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] args) {
        try {
            // Setup JDA
            JDA jda = JDABuilder.createDefault(System.getProperty("botToken"))
                    .enableIntents(
                            GatewayIntent.GUILD_VOICE_STATES,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS
//                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .setActivity(Activity.listening("music \uD83C\uDFB6"))
                    .addEventListeners(new BotApplicationManager())
                    .build();

            jda.awaitReady();

            // Setup commands
            jda.updateCommands().addCommands(
//                    Commands.slash("playlocal", "play local track"),
                    Commands.slash("help", "Shows the help embed box.")
                            .setGuildOnly(true),
                    Commands.slash("play", "Starts playing something.")
                            .setGuildOnly(true)
                            .addOptions(
                                    new OptionData(OptionType.STRING, "query", "Could be a song, playlist or album name or link. YouTube & Spotify.")
                                            .setRequired(true)),
                    Commands.slash("stop", "Stop player.")
                            .setGuildOnly(true),
                    Commands.slash("pause", "Pause current playing track. Use again to unpause.")
                            .setGuildOnly(true),
                    Commands.slash("resume", "Resume player.")
                            .setGuildOnly(true),
                    Commands.slash("shuffle", "Shuffles the queue.")
                            .setGuildOnly(true),
                    Commands.slash("skip", "Skips the current song.")
                            .setGuildOnly(true),
                    Commands.slash("dc", "Disconnect player. Alternative: /leave")
                            .setGuildOnly(true),
                    Commands.slash("leave", "Disconnect player. Alternative: /dc")
                            .setGuildOnly(true),
                    Commands.slash("playnow", "Destroys current queue and plays whatever provided.")
                            .setGuildOnly(true)
                            .addOptions(
                                    new OptionData(OptionType.STRING, "query", "Could be a song, playlist or album name or link. YouTube & Spotify.")
                                            .setRequired(true)),
                    Commands.slash("playnext", "Adds in queue whatever provided right after the current track.")
                            .setGuildOnly(true)
                            .addOptions(
                                    new OptionData(OptionType.STRING, "query", "Could be a song, playlist or album name or link. YouTube & Spotify.")
                                            .setRequired(true)),
                    Commands.slash("song", "Shows current playing track.")
                            .setGuildOnly(true),
                    Commands.slash("queue", "Display current queue list.")
                            .setGuildOnly(true),
                    Commands.slash("clearq", "Clears the queue.")
                            .setGuildOnly(true),
                    Commands.slash("volume", "Set the volume player.")
                            .setGuildOnly(true)
                            .addOptions(
                                    new OptionData(OptionType.INTEGER, "value", "0-100")
                                            .setRequired(true)
                                            .setRequiredRange(0, 100)),
                    Commands.slash("forward", "Forward track by given seconds.")
                            .setGuildOnly(true)
                            .addOptions(
                                    new OptionData(OptionType.INTEGER, "seconds", "Number of seconds you want to forward.")
                                            .setRequired(true)),
                    Commands.slash("backward", "Backward track by given seconds.")
                            .setGuildOnly(true)
                            .addOptions(
                                    new OptionData(OptionType.INTEGER, "seconds", "Number of seconds you want to forward.")
                                            .setRequired(true)),
                    Commands.slash("setoutputchannel", "[ADMIN] Set output channel to current message channel.")
                            .setGuildOnly(true)
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            ).queue();

            Gson gson = new Gson();

            /**
             * Example API GET request
             * Get all servers and print their Guild ID and Name
             */
            String get = WebReq.Get("/servers/all");
            Response r = gson.fromJson(get, Response.class);

            if (r.getCode() == StatusCodes.OK.getCode()) {
                Server[] servers = gson.fromJson(gson.toJson(r.getData()), Server[].class);
                for (Server server : servers) {
                    System.out.println(server.getGuildId() + " " + server.getName());
                }
            }

            System.out.println("Logged in as " + jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator());
        } catch (Exception e) {
            e.printStackTrace();
//            System.exit(-1);
        }
    }
}
