package bot;

import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.initialization.SetupCommands;
import bot.listeners.BotApplicationManager;
import bot.api.entities.Response;
import bot.api.entities.Server;
import bot.listeners.ButtonComponentClick;
import bot.listeners.GeneralEvents;
import com.google.gson.Gson;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

public class Main {
    public static void main(String[] args) {
        try {
            BotApplicationManager applicationManager = new BotApplicationManager();
            ButtonComponentClick buttonComponentClick = new ButtonComponentClick(applicationManager);

            ShardManager shardManager = DefaultShardManagerBuilder.createDefault(System.getProperty("botToken"))
                    .enableIntents(
                            GatewayIntent.GUILD_VOICE_STATES,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .setAudioSendFactory(new NativeAudioSendFactory())
                    .setActivity(Activity.listening("music \uD83C\uDFB6"))
                    .addEventListeners(applicationManager, new GeneralEvents(), buttonComponentClick)
                    .build();

            JDA jda = shardManager.getShards().get(0);
            SetupCommands.Setup(jda);

            Gson gson = new Gson();

            /*
              Example API GET request
              Get all servers and print their Guild ID and Name
             */
            String get = WebReq.Get("/servers/all");
            Response r = gson.fromJson(get, Response.class);

            if (r.getCode() == StatusCodes.OK.getCode()) {
                Server[] servers = gson.fromJson(gson.toJson(r.getData()), Server[].class);
                for (Server server : servers) {
                    System.out.println(server.getGuildId() + " " + server.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
//            System.exit(-1);
        }
    }
}
