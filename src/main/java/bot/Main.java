package bot;

import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.listeners.BotApplicationManager;
import bot.listeners.CommandManager;
import bot.dto.Response;
import bot.dto.Server;
import com.google.gson.Gson;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

public class Main {
    public static void main(String[] args) {
        try {
            ShardManager shardManager = DefaultShardManagerBuilder.createDefault(System.getProperty("botToken"))
                    .enableIntents(
                            GatewayIntent.GUILD_VOICE_STATES,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .setActivity(Activity.listening("music \uD83C\uDFB6"))
                    .addEventListeners(new BotApplicationManager(), new CommandManager())
                    .build();

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
