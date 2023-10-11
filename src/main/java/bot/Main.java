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
import dev.arbjerg.lavalink.client.*;
import dev.arbjerg.lavalink.client.loadbalancing.RegionGroup;
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;
import dev.arbjerg.lavalink.protocol.v4.Message;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.net.URI;
import java.util.List;

public class Main {
    private final LavalinkClient lavalinkClient;

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        lavalinkClient = new LavalinkClient(Helpers.getUserIdFromToken(System.getProperty("botToken")));
        lavalinkClient.getLoadBalancer().addPenaltyProvider(new VoiceRegionPenaltyProvider());

        this.registerLavalinkListeners();
        this.registerLavalinkNodes();

        ShardManager shardManager = DefaultShardManagerBuilder.createDefault(System.getProperty("botToken"))
                .enableIntents(
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .setActivity(Activity.listening("music \uD83C\uDFB6"))
                .setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(lavalinkClient))
                .build();

//            lavakord.addNode("ws://135.125.190.158:2333", "youshallnotpass");
//            lavakord.addNode("ws://localhost:2333", "youshallnotpass");

        BotApplicationManager applicationManager = new BotApplicationManager(lavalinkClient);
        ButtonComponentClick buttonComponentClick = new ButtonComponentClick(applicationManager);
        GeneralEvents generalEvents = new GeneralEvents();
        shardManager.addEventListener(applicationManager, buttonComponentClick, generalEvents);

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
    }

    private void registerLavalinkListeners() {
        this.lavalinkClient.on(dev.arbjerg.lavalink.client.ReadyEvent.class).subscribe((data) -> {
            final LavalinkNode node = data.getNode();
            final Message.ReadyEvent event = data.getEvent();

            System.out.printf(
                    "Node '%s' is ready, session id is '%s'!%n",
                    node.getName(),
                    event.getSessionId()
            );
        });

        this.lavalinkClient.on(StatsEvent.class).subscribe((data) -> {
            final LavalinkNode node = data.getNode();
            final Message.StatsEvent event = data.getEvent();

            System.out.printf(
                    "Node '%s' has stats, current players: %d/%d%n",
                    node.getName(),
                    event.getPlayingPlayers(),
                    event.getPlayers()
            );
        });
    }

    private void registerLavalinkNodes() {
        List.of(
                lavalinkClient.addNode(
                        "Testnode",
                        URI.create("ws://192.168.1.4:2333"),
                        "youshallnotpass",
                        RegionGroup.EUROPE
                )
//                lavalinkClient.addNode(
//                        "OVHvps",
//                        URI.create("ws://135.125.190.158:2333"),
//                        "youshallnotpass",
//                        RegionGroup.EUROPE
//                )
        ).forEach((node) -> {
            node.on(TrackStartEvent.class).subscribe((data) -> {
                final LavalinkNode node1 = data.getNode();
                final var event = data.getEvent();

                System.out.printf(
                        "%s: track started: %s%n",
                        node1.getName(),
                        event.getTrack().getInfo()
                );
            });
        });
    }
}
