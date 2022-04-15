package bot;

import bot.controller.BotCommandMappingHandler;
import bot.controller.BotController;
import bot.controller.BotControllerManager;
import bot.music.MusicController;
import com.github.topislavalinkplugins.topissourcemanagers.spotify.SpotifyConfig;
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
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BotApplicationManager extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(BotApplicationManager.class);

    private final Map<Long, BotGuildContext> guildContexts;
    private final BotControllerManager controllerManager;
    private final AudioPlayerManager playerManager;
    private final ScheduledExecutorService executorService;

    private final String ipv6Block = System.getProperty("ipv6Block");

    public BotApplicationManager() {
        guildContexts = new HashMap<>();
        controllerManager = new BotControllerManager();

        controllerManager.registerController(new MusicController.Factory());

//        SpotifySingleton.Init(System.getProperty("spotifyClientId"), System.getProperty("spotifyClientSecret"));

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
        playerManager.registerSourceManager(new com.github.topislavalinkplugins.topissourcemanagers.spotify.SpotifySourceManager(
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

//    public Spotify getSpotify() {
//        return spotify;
//    }

    private BotGuildContext createGuildState(long guildId, Guild guild) {
        BotGuildContext context = new BotGuildContext(guildId);

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

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        Member member = event.getMember();

        if (!event.isFromType(ChannelType.TEXT) || member == null || member.getUser().isBot()) {
            return;
        }

        BotGuildContext guildContext = getContext(event.getGuild());

        controllerManager.dispatchMessage(guildContext.controllers, System.getProperty("prefix"), event.getMessage(), new BotCommandMappingHandler() {
            @Override
            public void commandNotFound(Message message, String name) {

            }

            @Override
            public void commandWrongParameterCount(Message message, String name, String usage, int given, int required) {
                event.getTextChannel().sendMessage("Wrong argument count for command").queue();
            }

            @Override
            public void commandWrongParameterType(Message message, String name, String usage, int index, String value, Class<?> expectedType) {
                event.getTextChannel().sendMessage("Wrong argument type for command").queue();
            }

            @Override
            public void commandRestricted(Message message, String name) {
                event.getTextChannel().sendMessage("Command not permitted").queue();
            }

            @Override
            public void commandException(Message message, String name, Throwable throwable) {
                event.getTextChannel().sendMessage("Command threw an exception").queue();

                log.error("Command with content {} threw an exception.", message.getContentDisplay(), throwable);
            }

        });
    }

    private boolean isAlone(Guild guild)
    {
        if(guild.getAudioManager().getConnectedChannel() == null) return false;
        return guild.getAudioManager().getConnectedChannel().getMembers().stream()
                .noneMatch(x ->
                        !x.getVoiceState().isDeafened()
                                && !x.getUser().isBot());
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
//        super.onGuildVoiceUpdate(event);

//        BotGuildContext guildContext = getContext(event.getGuild());
//
//        Guild guild = event.getEntity().getGuild();
//        if (isAlone(guild)) {
//            controllerManager.destroyPlayer(guildContext.controllers);
//        }
    }

    @Override
    public void onGuildVoiceLeave(final GuildVoiceLeaveEvent event) {
        BotGuildContext guildContext = getContext(event.getGuild());

        // Get number of members in voice channel
        // If there's only one member in the channel, check if it's the bot.
        // If it is, disconnect the voice channel.
        if (!event.getMember().getUser().equals(event.getJDA().getSelfUser())
                && event.getChannelLeft().getMembers().size() == 1
                && event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember())) {
            controllerManager.destroyPlayer(guildContext.controllers);
            return;
        }

        // If the bot leaves a voice channel, destroy player.
        // But before doing so, check if the left channel has 0 members.
        // If it does, this means that the player has already been destroyed
        // because the bot left the channel due to nobody being in it. (see above)
        if (event.getMember().getUser().equals(event.getJDA().getSelfUser())) {
            controllerManager.destroyPlayer(guildContext.controllers);
            return;
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        // do stuff
    }
}
