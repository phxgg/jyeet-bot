import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;

public class Main extends ListenerAdapter {
    public static void main(String args[]) {
        final String botToken = "ODkxNzU4NzU2MzE1MTY0Njgy.YVDBDw.MC03pWhSReU5jTqv87ZkFpY3sdg";

        try {
            JDA jda = JDABuilder.createDefault(botToken)
                    .setActivity(Activity.playing("Your mom's clit"))
                    .addEventListeners(new Main())
                    .build();

            jda.awaitReady();

            System.out.println("Finished Building JDA!");
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    private Main() {
        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.TEXT))
            return;

        if (event.getAuthor().isBot())
            return;

        String[] command = event.getMessage().getContentRaw().split(" ", 2);

        switch (command[0]) {
            case ".play":
                if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.VOICE_CONNECT)) {
                    event.getTextChannel().sendMessage("I do not have permissions to join a voice channel!").queue();
                }

                assert(event.getMember() != null);
                VoiceChannel connectedChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
                if (connectedChannel == null) {
                    event.getTextChannel().sendMessage("You are not connected to a voice channel!").queue();
                    return;
                }

                if (command.length != 2)
                    return;

                loadAndPlay(connectedChannel, event.getTextChannel(), command[1]);
                break;
            case ".skip":
                skipTrack(event.getTextChannel());
                break;
            case ".stop":
                stop(event.getTextChannel());
                break;
            case ".song":
                song(event.getTextChannel());
                break;
        }

        super.onMessageReceived(event);
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());
        musicManager.player.destroy();
        event.getGuild().getAudioManager().closeAudioConnection();
        // destroy player
    }

    private void loadAndPlay(final VoiceChannel voiceChannel, final TextChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

                play(voiceChannel, channel.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(voiceChannel, channel.getGuild(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(VoiceChannel voiceChannel, Guild guild, GuildMusicManager musicManager, AudioTrack track) {
//        connectToFirstVoiceChannel(guild.getAudioManager());
        guild.getAudioManager().openAudioConnection(voiceChannel);

        musicManager.scheduler.queue(track);
    }

    private void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();

        channel.sendMessage("Skipped to next track.").queue();
    }

    private void stop(TextChannel channel) {
        Guild guild = channel.getGuild();
        GuildMusicManager musicManager = getGuildAudioPlayer(guild);

//        musicManager.scheduler.clearQueue();
        musicManager.player.destroy();
        guild.getAudioManager().closeAudioConnection();

        channel.sendMessage("Stopped player.").queue();
    }

    private void song(TextChannel channel) {
        Guild guild = channel.getGuild();
        GuildMusicManager musicManager = getGuildAudioPlayer(guild);
        AudioTrackInfo current = musicManager.player.getPlayingTrack().getInfo();

        channel.sendMessage("Currently playing " + current.title + " by " + current.author).queue();
    }

    private static void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected()) { // && !audioManager.isAttemptingToConnect()
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
        }
    }
}
