package bot;

import bot.apis.spotify.SpotifySingleton;
import bot.controller.BotCommandMappingHandler;
import bot.controller.BotController;
import bot.controller.BotControllerManager;
import bot.music.MusicController;
import bot.sources.spotify.SpotifyAudioSourceManager;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BotApplicationManager extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(BotApplicationManager.class);

    private final Map<Long, BotGuildContext> guildContexts;
    private final BotControllerManager controllerManager;
    private final AudioPlayerManager playerManager;
    private final ScheduledExecutorService executorService;

//    private final Spotify spotify = new Spotify();

    public BotApplicationManager() {
        guildContexts = new HashMap<>();
        controllerManager = new BotControllerManager();

        controllerManager.registerController(new MusicController.Factory());

        SpotifySingleton.init("c568993de30842e78c598306469aa613", "34040d4b2975409187928f90c596cca6");
        YoutubeAudioSourceManager yasm = new YoutubeAudioSourceManager();

        playerManager = new DefaultAudioPlayerManager();
//        playerManager.useRemoteNodes("localhost:8080");
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
//        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(new SpotifyAudioSourceManager(yasm));
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

        controllerManager.dispatchMessage(guildContext.controllers, ".", event.getMessage(), new BotCommandMappingHandler() {
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
