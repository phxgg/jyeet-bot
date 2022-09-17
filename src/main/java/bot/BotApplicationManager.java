package bot;

import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.controller.BotController;
import bot.controller.BotControllerManager;
import bot.controller.BotSlashCommandMappingHandler;
import bot.dto.Response;
import bot.dto.Server;
import bot.music.MusicController;
import com.github.topislavalinkplugins.topissourcemanagers.spotify.SpotifyConfig;
import com.github.topislavalinkplugins.topissourcemanagers.spotify.SpotifySourceManager;
import com.google.gson.Gson;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
//    private final ScheduledThreadPoolExecutor executorService; // use for setRemoveOnCancelPolicy(true)
    private final Gson gson;

    private final String ipv6Block = System.getProperty("ipv6Block");

    public BotApplicationManager() {
        gson = new Gson();
        guildContexts = new HashMap<>();
        controllerManager = new BotControllerManager();

        controllerManager.registerController(new MusicController.Factory());

        SpotifyConfig spotifyConfig = new SpotifyConfig();
        spotifyConfig.setClientId(System.getProperty("spotifyClientId"));
        spotifyConfig.setClientSecret(System.getProperty("spotifyClientSecret"));
        spotifyConfig.setCountryCode("GR");

        YoutubeAudioSourceManager yasm = new YoutubeAudioSourceManager();

        if (ipv6Block != null && !ipv6Block.isEmpty()) {
            @SuppressWarnings("rawtypes") List<IpBlock> blocks = List.of(new Ipv6Block(ipv6Block));
            RotatingNanoIpRoutePlanner planner = new RotatingNanoIpRoutePlanner(blocks);
            new YoutubeIpRotatorSetup(planner)
                    .withRetryLimit(6)
                    .forSource(yasm).setup();
        }
//        Optional<Properties> opt = readYoutubeConfig();
//        if (opt.isPresent()) {
//            Properties props = opt.get();
//            String PSID = props.getProperty("PSID");
//            String PAPISID = props.getProperty("PAPISID");
//            YoutubeHttpContextFilter.setPSID(PSID);
//            YoutubeHttpContextFilter.setPAPISID(PAPISID);
//        }

        playerManager = new DefaultAudioPlayerManager();
//        playerManager.useRemoteNodes("localhost:8080");
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
//        playerManager.registerSourceManager(new SpotifyAudioSourceManager(yasm));
        playerManager.registerSourceManager(new SpotifySourceManager(
                null, spotifyConfig, playerManager
        ));
        playerManager.registerSourceManager(yasm);
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager());
        playerManager.registerSourceManager(new LocalAudioSourceManager());

        executorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("bot"));

        // Use this if you want to use setRemoveOnCancelPolicy
//        executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, new DaemonThreadFactory("bot"));
//        executorService.setRemoveOnCancelPolicy(true);
    }

//    public static Optional<Properties> readYoutubeConfig() {
//        Properties properties = new Properties();
//        try (InputStream in = Main.class.getResourceAsStream("/youtube.properties")) {
//            properties.load(in);
//            return Optional.of(properties);
//        } catch (IOException e) {
//            return Optional.empty();
//        }
//    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
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

        for (BotController controller : controllerManager.createControllers(this, context, guild)) {
            context.controllers.put(controller.getClass(), controller);
        }

        return context;
    }

    private synchronized BotGuildContext getContext(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        BotGuildContext context = guildContexts.get(guildId);

        if (context == null) {
            context = createGuildState(guildId, guild);
            guildContexts.put(guildId, context);
        }

        return context;
    }

    /**
     * ====================================
     * EVENTS
     * ====================================
     */

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

        String prefix = guildContext.guildPrefix;

        controllerManager.dispatchSlashCommand(guildContext.controllers, event, new BotSlashCommandMappingHandler() {
            @Override
            public void commandNotFound(String name) {

            }

            @Override
            public void commandWrongParameterCount(String name, String usage, int given, int required) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription("Wrong argument count.");
                eb.setFooter("Command: " + name);

//                event.getTextChannel().sendMessageEmbeds(eb.build()).queue();
                event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();
            }

            @Override
            public void commandWrongParameterType(String name, String usage, int index, String value, Class<?> expectedType) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription("Wrong argument type.");
                eb.setFooter("Command: " + name);

                event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();
            }

            @Override
            public void commandRestricted(String name) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription("Command not permitted.");
                eb.setFooter("Command: " + name);

                event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();
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

                event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();

//                log.error("Command with content {} threw an exception.", message.getContentDisplay(), throwable);
            }
        });

        event.getHook().deleteOriginal().queue();
    }

    @Override
    public void onGuildVoiceUpdate(@Nonnull GuildVoiceUpdateEvent event) {
        BotGuildContext guildContext = getContext(event.getGuild());

        // Get number of members in voice channel
        // If there's only one member in the channel, check if it's the bot.
        // If it is, disconnect the voice channel.

        // Fix warnings
        if (event.getChannelLeft() == null)
            return;

        if (!event.getMember().getUser().equals(event.getJDA().getSelfUser())
                && event.getChannelLeft().getMembers().size() == 1
                && event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember())) {
            controllerManager.destroyPlayer(guildContext.controllers);

            /* TODO:
                Wait in VC alone for some time before disconnecting,
                but let the bot be able to start playing in a new channel if asked to while being alone in a VC.
                Look into: controllerManager.waitInVC(guildContext.controllers);
            */

            return;
        }
    }

    @Override
    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
        BotGuildContext guildContext = getContext(event.getGuild());

        // If the bot leaves a voice channel, destroy player.
        if (event.getMember().getUser().equals(event.getJDA().getSelfUser())) {
            controllerManager.destroyPlayer(guildContext.controllers);
            return;
        }
    }

    @Override
    public void onGuildLeave(@Nonnull GuildLeaveEvent event) {
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
    public void onGuildJoin(@Nonnull GuildJoinEvent event) {
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
