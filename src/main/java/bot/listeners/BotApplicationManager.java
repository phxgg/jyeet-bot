package bot.listeners;

import bot.records.BotGuildContext;
import bot.records.MessageType;
import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.controller.IBotController;
import bot.controller.BotControllerManager;
import bot.controller.IBotSlashCommandMappingHandler;
import bot.api.entities.Response;
import bot.api.entities.Server;
import bot.controller.impl.music.MusicController;
import bot.records.SpotifyConfig;
import bot.controller.impl.utility.UtilityController;
//import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.github.topi314.lavasrc.spotify.SpotifySourceManager;
import com.google.gson.Gson;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.BalancingIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidWithThumbnail;
import dev.lavalink.youtube.clients.MusicWithThumbnail;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.WebWithThumbnail;
import dev.lavalink.youtube.clients.skeleton.Client;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BotApplicationManager extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(BotApplicationManager.class);

    private final Map<Long, BotGuildContext> guildContexts;
    private final BotControllerManager controllerManager;
    private final AudioPlayerManager playerManager;
    private final ScheduledExecutorService executorService; // Interface
    private final Gson gson;

    private final String ipv6Block = System.getProperty("ipv6Block");

    public BotApplicationManager() {
        gson = new Gson();
        guildContexts = new HashMap<>();
        controllerManager = new BotControllerManager();

        controllerManager.registerController(new MusicController.Factory());
        controllerManager.registerController(new UtilityController.Factory());
//        controllerManager.registerController(new BankController.Factory());

        SpotifyConfig spotifyConfig = new SpotifyConfig();
        spotifyConfig.setClientId(System.getProperty("spotifyClientId"));
        spotifyConfig.setClientSecret(System.getProperty("spotifyClientSecret"));
        spotifyConfig.setCountryCode("GR");

        // same as the 'common' module but there are additional clients that provide video thumbnails in the returned metadata.
        String poToken = System.getProperty("poToken");
        String visitorData = System.getProperty("visitorData");
        if (poToken != null && !poToken.isEmpty()
                && visitorData != null && !visitorData.isEmpty()
        ) {
            Web.setPoTokenAndVisitorData(poToken, visitorData);
        }
        YoutubeAudioSourceManager yasm = new YoutubeAudioSourceManager(/*allowSearch:*/ true, new Client[] { new MusicWithThumbnail(), new WebWithThumbnail(), new AndroidWithThumbnail() });
//        YoutubeAudioSourceManager yasm = new YoutubeAudioSourceManager();

        if (ipv6Block != null && !ipv6Block.isEmpty()) {
            @SuppressWarnings("rawtypes") List<IpBlock> blocks = List.of(new Ipv6Block(ipv6Block));
//            RotatingNanoIpRoutePlanner planner = new RotatingNanoIpRoutePlanner(blocks);
            BalancingIpRoutePlanner planner = new BalancingIpRoutePlanner(blocks);
            YoutubeIpRotatorSetup rotator = new YoutubeIpRotatorSetup(planner);
            rotator.forConfiguration(yasm.getHttpInterfaceManager(), false)
                    .withRetryLimit(10)
                    .withMainDelegateFilter(null)
                    .setup();
        }

        playerManager = new DefaultAudioPlayerManager();
//        playerManager.useRemoteNodes("localhost:8080");
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.registerSourceManager(new SpotifySourceManager(
                null,
                spotifyConfig.getClientId(),
                spotifyConfig.getClientSecret(),
                spotifyConfig.getCountryCode(),
                playerManager
        ));
        playerManager.registerSourceManager(yasm);
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new GetyarnAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));
        playerManager.registerSourceManager(new LocalAudioSourceManager());

        executorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("bot"));

        // Use this if you want to use setRemoveOnCancelPolicy
//        executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, new DaemonThreadFactory("bot"));
//        executorService.setRemoveOnCancelPolicy(true);
    }

    public ScheduledExecutorService getExecutorService() {
        return this.executorService;
    }

    public AudioPlayerManager getPlayerManager() {
        return this.playerManager;
    }

    private BotGuildContext createGuildState(long guildId, Guild guild) {
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", String.valueOf(guildId));
        }};

        String post = WebReq.Post("/servers/guild", data);
        Response r = gson.fromJson(post, Response.class);

        String prefix;

        if (r.getCode() == StatusCodes.OK.getCode()) {
            Server server = gson.fromJson(gson.toJson(r.getData()), Server.class);
            prefix = server.getPrefix();
        } else {
            prefix = System.getProperty("prefix");
        }

        BotGuildContext context = new BotGuildContext(guildId, prefix);

        for (IBotController controller : controllerManager.createControllers(this, context, guild)) {
            context.getControllers().put(controller.getClass(), controller);
        }

        return context;
    }

    public synchronized BotGuildContext getContext(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        BotGuildContext context = guildContexts.get(guildId);

        if (context == null) {
            context = createGuildState(guildId, guild);
            guildContexts.put(guildId, context);
        }

        return context;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        Member member = event.getMember();

        if (event.getGuild() == null || member == null || member.getUser().isBot()) {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setTitle("Error");
            eb.setColor(MessageType.Error.color);
            eb.setDescription("You can't use commands in DMs.");

            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        BotGuildContext guildContext = getContext(event.getGuild());

        String prefix = guildContext.getGuildPrefix();

        controllerManager.dispatchSlashCommand(guildContext.getControllers(), event, new IBotSlashCommandMappingHandler() {
            @Override
            public void commandNotFound(String name) {

            }

            @Override
            public void commandWrongParameterCount(String name, String description, String usage, int given, int required) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription("Wrong argument count.");
                eb.setFooter("Command: " + name);

//                event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();
                event.getHook().setEphemeral(true).editOriginalEmbeds(eb.build()).queue();
            }

            @Override
            public void commandWrongParameterType(String name, String description, String usage, int index, String value, Class<?> expectedType) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription("Wrong argument type.");
                eb.setFooter("Command: " + name);

                event.getHook().setEphemeral(true).editOriginalEmbeds(eb.build()).queue();
            }

            @Override
            public void commandRestricted(String name) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription("Command not permitted.");
                eb.setFooter("Command: " + name);

                event.getHook().setEphemeral(true).editOriginalEmbeds(eb.build()).queue();
            }

            @Override
            public void commandException(String name, Throwable throwable) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription(
                        String.format("Command threw an exception:\n`%s`\n```%s```",
                                throwable.getClass().getSimpleName(),
                                throwable.getMessage())
                );
                eb.setFooter("Command: " + name);

                event.getHook().setEphemeral(true).editOriginalEmbeds(eb.build()).queue();

//                log.error("Command with content {} threw an exception.", message.getContentDisplay(), throwable);
            }
        });

//        event.getHook().deleteOriginal().queue();
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        BotGuildContext guildContext = getContext(event.getGuild());

        // Get number of members in voice channel
        // If there's only one member in the channel, check if it's the bot.
        // If it is, disconnect the voice channel.

        // Fix warnings
        if (event.getChannelLeft() == null)
            return;

        // If the bot leaves a voice channel, destroy player.
        if (event.getMember().getUser().equals(event.getJDA().getSelfUser())) {
            controllerManager.destroyPlayer(guildContext.getControllers());
            return;
        }

        if (!event.getMember().getUser().equals(event.getJDA().getSelfUser())
                && event.getChannelLeft().getMembers().size() == 1
                && event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember())) {
            controllerManager.destroyPlayer(guildContext.getControllers());

            /* TODO:
                Wait in VC alone for some time before disconnecting,
                but let the bot be able to start playing in a new channel if asked to while being alone in a VC.
                Look into: controllerManager.waitInVC(guildContext.controllers);
            */

            return;
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        // Delete guild from database
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", event.getGuild().getId());
        }};

        String post = WebReq.Post("/servers/deleteGuild", data);
        Response r = gson.fromJson(post, Response.class);

        if (r.getCode() == StatusCodes.OK.getCode()) {
            log.info("[{}] Server deleted.", event.getGuild().getName());
        } else {
            log.error("[{}] Server could not be deleted.", event.getGuild().getName());
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        // Add guild in database
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", event.getGuild().getId());
            put("ownerId", event.getGuild().getOwnerId());
            put("name", event.getGuild().getName());
            put("prefix", System.getProperty("prefix"));
        }};

        String post = WebReq.Post("/servers/create", data);
        Response r = gson.fromJson(post, Response.class);

        if (r.getCode() == StatusCodes.OK.getCode()) {
            log.info("[{}] Server created.", event.getGuild().getName());
        } else {
            log.error("[{}] Server could not be created.", event.getGuild().getName());
        }
    }

    @Override
    public void onGuildUpdateName(@NotNull GuildUpdateNameEvent event) {
        // Update guild name in database
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", event.getGuild().getId());
            put("name", event.getGuild().getName());
        }};

        String post = WebReq.Post("/servers/updateGuildName", data);
        Response r = gson.fromJson(post, Response.class);

        if (r.getCode() == StatusCodes.OK.getCode()) {
            log.info("[{}] Updated name.", event.getGuild().getName());
        } else {
            log.error("[{}] Could not update name.", event.getGuild().getName());
        }
    }

    @Override
    public void onGuildUpdateOwner(@NotNull GuildUpdateOwnerEvent event) {
        // Update guild owner in database
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", event.getGuild().getId());
            put("ownerId", event.getGuild().getOwnerId());
        }};

        String post = WebReq.Post("/servers/updateGuildOwner", data);
        Response r = gson.fromJson(post, Response.class);

        if (r.getCode() == StatusCodes.OK.getCode()) {
            log.info("[{}] Updated ownerId.", event.getGuild().getName());
        } else {
            log.error("[{}] Could not update ownerId.", event.getGuild().getName());
        }
    }
}
