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
import dev.schlaubi.lavakord.interop.JavaInterop;
import dev.schlaubi.lavakord.interop.JavaLavakord;
import dev.schlaubi.lavakord.interop.jda.LavakordDefaultShardManagerBuilder;
import dev.schlaubi.lavakord.jda.LShardManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

public class Main {
    public static void main(String[] args) {
        try {

            LShardManager container = new LavakordDefaultShardManagerBuilder(
                    DefaultShardManagerBuilder.createDefault(System.getProperty("botToken"))
                            .enableIntents(
                                    GatewayIntent.GUILD_VOICE_STATES,
                                    GatewayIntent.GUILD_MESSAGES,
                                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                                    GatewayIntent.GUILD_MEMBERS,
                                    GatewayIntent.MESSAGE_CONTENT
                            )
                            .setActivity(Activity.listening("music \uD83C\uDFB6"))
            ).build();

            JavaLavakord lavakord = JavaInterop.createJavaInterface(container.getLavakord());
//            lavakord.addNode("ws://135.125.190.158:2333", "youshallnotpass");
            lavakord.addNode("ws://localhost:2333", "youshallnotpass");

            BotApplicationManager applicationManager = new BotApplicationManager(lavakord);
            ButtonComponentClick buttonComponentClick = new ButtonComponentClick(applicationManager);
            GeneralEvents generalEvents = new GeneralEvents();
            container.getShardManager().addEventListener(applicationManager, buttonComponentClick, generalEvents);

            JDA jda = container.getShardManager().getShards().get(0);
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
