package bot;

import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.controller.IBotController;
import bot.initialization.SetupCommands;
import bot.listeners.BotApplicationManager;
import bot.api.entities.Response;
import bot.api.entities.Server;
import bot.listeners.ButtonComponentClick;
import bot.listeners.GeneralEvents;
import bot.music.MusicController;
import bot.records.BotGuildContext;
import bot.records.MessageType;
import bot.utility.UtilityController;
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
            Need to call updateTrackBox(false) on player pause/resume.
         */

        // TODO: Implement history queue.

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
        this.lavalinkClient.on(PlayerUpdateEvent.class).subscribe((data) -> {
            final var event = data.getEvent();
            System.out.printf("Connected: %s%n", event.getState().getConnected());
            System.out.printf("OP: %s%n", event.getOp().getValue());
            System.out.printf("Ping: %s%n", event.getState().getPing());
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
                forController(MusicController.class, event.getGuildId(), (controller) -> {
                    MusicController cast = (MusicController) controller;
                    cast.getScheduler().updateTrackBox(true);
                });
            });
            node.on(TrackEndEvent.class).subscribe((data) -> {
                final var event = data.getEvent();
                forController(MusicController.class, event.getGuildId(), (controller) -> {
                    MusicController cast = (MusicController) controller;
                    cast.getLink().getPlayer().flatMap(player -> player.clearEncodedTrack().asMono()).block();
//                    controller.getPlayer().setEncodedTrack(null).asMono().block();
                    if (event.getReason().getMayStartNext()) {
                        cast.getScheduler().startNextTrack(true);
                        cast.getMessageDispatcher().sendDisposableMessage(MessageType.Info, String.format("Track **%s** finished.", event.getTrack().getInfo().getTitle()));
                    }
                });
            });
            node.on(TrackStuckEvent.class).subscribe((data) -> {
                final var event = data.getEvent();
                forController(MusicController.class, event.getGuildId(), (controller) -> {
                    MusicController cast = (MusicController) controller;
                    cast.getMessageDispatcher().sendDisposableMessage(MessageType.Warning, String.format("Track **%s** got stuck, skipping.", event.getTrack().getInfo().getTitle()));
                    cast.getScheduler().startNextTrack(false);
                });
            });
            node.on(TrackExceptionEvent.class).subscribe((data) -> {
                final var event = data.getEvent();
                forController(MusicController.class, event.getGuildId(), (controller) -> {
                    MusicController cast = (MusicController) controller;
                    cast.getMessageDispatcher().sendDisposableMessage(MessageType.Warning, String.format("Track **%s** got exception, skipping.", event.getTrack().getInfo().getTitle()));
                    cast.getScheduler().startNextTrack(false);
                });
            });
            node.on(WebSocketClosedEvent.class).subscribe((data) -> {
                final var event = data.getEvent();
                forController(MusicController.class, event.getGuildId(), (controller) -> {
                    MusicController cast = (MusicController) controller;
                    cast.getGuild().getJDA().getDirectAudioController().disconnect(cast.getGuild());
                });
            });
        });
    }

    private void forController(Class<? extends IBotController> controllerClass, long guildId, GuildOperation operation) {
        BotGuildContext context = appManager.getContextById(guildId);
        if (context != null) {
            IBotController controller = context.getControllers().get(controllerClass);
            if (controller != null) {
                operation.execute(controller);
            }
        }
    }

    private void forController(Class<? extends IBotController> controllerClass, String guildId, GuildOperation operation) {
        forController(controllerClass, Long.parseLong(guildId), operation);
    }

    private interface GuildOperation {
        void execute(IBotController controller);
    }
}
