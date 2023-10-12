package bot;

import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.initialization.SetupCommands;
import bot.listeners.BotApplicationManager;
import bot.api.entities.Response;
import bot.api.entities.Server;
import bot.listeners.ButtonComponentClick;
import bot.listeners.GeneralEvents;
import bot.music.MusicController;
import bot.records.BotGuildContext;
import bot.records.MessageType;
import com.google.gson.Gson;
import dev.arbjerg.lavalink.client.*;
import dev.arbjerg.lavalink.client.loadbalancing.RegionGroup;
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;
import dev.arbjerg.lavalink.protocol.v4.Message;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.net.URI;
import java.util.List;

public class Main {
    private final LavalinkClient lavalinkClient;
    private final BotApplicationManager appManager;
    private final ButtonComponentClick buttonComponentClick;
    private final GeneralEvents generalEvents;

    public static void main(String[] args) {
        /* FIXME:
            If I start playing something, and then use the stop button in the trackbox,
            the next time I try to play anything it is not gonna send any audio.
            I have to manually disconnect the bot from the voice channel (or use /disconnect) and then use /play again.
            Also if that helps PlayerState.getConnected() returns false.
         */

        new Main();
    }

    public Main() {
        lavalinkClient = new LavalinkClient(Helpers.getUserIdFromToken(System.getProperty("botToken")));
        lavalinkClient.getLoadBalancer().addPenaltyProvider(new VoiceRegionPenaltyProvider());

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

        appManager = new BotApplicationManager(lavalinkClient);
        buttonComponentClick = new ButtonComponentClick(appManager);
        generalEvents = new GeneralEvents();
        shardManager.addEventListener(appManager, buttonComponentClick, generalEvents);

        this.registerLavalinkListeners();
        this.registerLavalinkNodes();

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
        this.lavalinkClient.on(ReadyEvent.class).subscribe((data) -> {
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
                        URI.create("ws://localhost:2333"),
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
                forMusicController(event.getGuildId(), (controller) -> {
                    System.out.printf("Track started playing %s%n", event.getTrack().getInfo().getTitle());
                    controller.getScheduler().updateTrackBox(true);
                });
            });
            node.on(TrackEndEvent.class).subscribe((data) -> {
                final var event = data.getEvent();
                forMusicController(event.getGuildId(), (controller) -> {
                    System.out.print("Track ended playing. " + event.getTrack().getInfo().getTitle());
                    controller.getPlayer().clearEncodedTrack().asMono().block();
                    if (event.getReason().getMayStartNext()) {
                        controller.getScheduler().startNextTrack(true);
                        controller.getMessageDispatcher().sendDisposableMessage(MessageType.Info, String.format("Track **%s** finished.", event.getTrack().getInfo().getTitle()));
                    }
                });
            });
            node.on(TrackStuckEvent.class).subscribe((data) -> {
                final var event = data.getEvent();
                forMusicController(event.getGuildId(), (controller) -> {
                    System.out.printf("Track got stuck %s%n", event.getTrack().getInfo().getTitle());
                    controller.getMessageDispatcher().sendDisposableMessage(MessageType.Warning, String.format("Track **%s** got stuck, skipping.", event.getTrack().getInfo().getTitle()));
                    controller.getScheduler().startNextTrack(false);
                });
            });
            node.on(TrackExceptionEvent.class).subscribe((data) -> {
                final var event = data.getEvent();
                forMusicController(event.getGuildId(), (controller) -> {
                    System.out.printf("Track got exception %s%n", event.getTrack().getInfo().getTitle());
                    controller.getMessageDispatcher().sendDisposableMessage(MessageType.Warning, String.format("Track **%s** got exception, skipping.", event.getTrack().getInfo().getTitle()));
                    controller.getScheduler().startNextTrack(false);
                });
            });
            node.on(WebSocketClosedEvent.class).subscribe((data) -> {
                final var event = data.getEvent();
                forMusicController(event.getGuildId(), (controller) -> {
                    System.out.printf("Websocket closed %s%n", event.getReason());
                    controller.getMessageDispatcher().sendDisposableMessage(MessageType.Error, String.format("WebSocket closed, stopping player. Reason:\n%s", event.getReason()));
                    controller.getLink().destroyPlayer().block();
                });
            });
            node.on(PlayerUpdateEvent.class).subscribe((data) -> {
                final var event = data.getEvent();
                System.out.printf("Connected: %s%n", data.getEvent().getState().getConnected());
                System.out.printf("OP: %s%n", data.getEvent().getOp().getValue());
                System.out.printf("Ping: %s%n", data.getEvent().getState().getPing());
            });
        });
    }

    private void forMusicController(long guildId, GuildOperation operation) {
        BotGuildContext context = appManager.getContextById(guildId);
        if (context != null) {
            MusicController controller = (MusicController) context.getControllers().get(MusicController.class);
            if (controller != null) {
                operation.execute(controller);
            }
        }
    }

    private void forMusicController(String guildId, GuildOperation operation) {
        forMusicController(Long.parseLong(guildId), operation);
    }

    private interface GuildOperation {
        void execute(MusicController controller);
    }
}
