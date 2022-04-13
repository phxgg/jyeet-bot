package bot.music;

import bot.BotApplicationManager;
import bot.BotGuildContext;
import bot.MessageDispatcher;
import bot.controller.BotCommandHandler;
import bot.controller.BotController;
import bot.controller.BotControllerFactory;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.managers.AudioManager;
import net.iharder.Base64;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class MusicController implements BotController {
    private static final float[] BASS_BOOST = { 0.2f, 0.15f, 0.1f, 0.05f, 0.0f, -0.05f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f,
            -0.1f, -0.1f, -0.1f, -0.1f };

    private final AudioPlayerManager manager;
    private final AudioPlayer player;
    private final AtomicReference<TextChannel> outputChannel;
    private final MusicScheduler scheduler;
    private final MessageDispatcher messageDispatcher;
    private final Guild guild;
    private final EqualizerFactory equalizer;

//    private final Spotify spotify;

    public MusicController(BotApplicationManager manager, BotGuildContext state, Guild guild) {
//        this.spotify = manager.getSpotify();
        this.manager = manager.getPlayerManager();
        this.guild = guild;
        this.equalizer = new EqualizerFactory();

        player = manager.getPlayerManager().createPlayer();
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        outputChannel = new AtomicReference<>();

        messageDispatcher = new GlobalDispatcher();
        scheduler = new MusicScheduler(player, messageDispatcher, manager.getExecutorService());

        player.addListener(scheduler);
    }

    public void destroyPlayer() {
        scheduler.clearQueue();
        player.setVolume(100);
        player.setFilterFactory(null);
        player.destroy();
//        guild.getAudioManager().setSendingHandler(null);
        guild.getAudioManager().closeAudioConnection();
        messageDispatcher.sendDisposableMessage("Player stopped.");
    }

    @BotCommandHandler
    private void play(Message message, String identifier) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        addTrack(message, identifier, false, false);
    }

    @BotCommandHandler
    private void p(Message message, String identifier) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        addTrack(message, identifier, false, false);
    }

    @BotCommandHandler
    private void playnow(Message message, String identifier) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        addTrack(message, identifier, true, false);
    }

    @BotCommandHandler
    private void queue(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        // TODO: Implement queue

        BlockingDeque<AudioTrack> _queue = scheduler.getQueue();
        if (_queue.isEmpty()) {
            messageDispatcher.sendDisposableMessage("The queue is empty.");
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Queue");
        eb.setColor(Color.CYAN);
        // for each track in the queue add a line to the embed
        int i = 1;
        for (AudioTrack track : _queue) {
            eb.addField(String.format("%d", i), String.format("%s", track.getInfo().title), true);
            i++;

            // Only display 10 tracks for now
            if (i > 10) {
                break;
            }
        }

        if (_queue.size() > i) {
            eb.setFooter(String.format("and %d more...", _queue.size()-i), null);
        }

        message.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    @BotCommandHandler
    private void hex(Message message, int pageCount) {
        manager.source(YoutubeAudioSourceManager.class).setPlaylistPageCount(pageCount);
    }

    @BotCommandHandler
    private void serialize(Message message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageOutput outputStream = new MessageOutput(baos);

        for (AudioTrack track : scheduler.drainQueue()) {
            manager.encodeTrack(outputStream, track);
        }

        outputStream.finish();

        message.getChannel().sendMessage(Base64.encodeBytes(baos.toByteArray())).queue();
    }

    @BotCommandHandler
    private void deserialize(Message message, String content) throws IOException {
        outputChannel.set((TextChannel) message.getChannel());
        connectToVoiceChannel(message, guild.getAudioManager());

        byte[] bytes = Base64.decode(content);

        MessageInput inputStream = new MessageInput(new ByteArrayInputStream(bytes));
        DecodedTrackHolder holder;

        while ((holder = manager.decodeTrack(inputStream)) != null) {
            if (holder.decodedTrack != null) {
                scheduler.addToQueue(holder.decodedTrack);
            }
        }
    }

    @BotCommandHandler
    private void volume(Message message, int volume) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        player.setVolume(volume);
    }

    @BotCommandHandler
    private void skip(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        scheduler.skip();
    }

    @BotCommandHandler
    private void next(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        scheduler.skip();
    }

    @BotCommandHandler
    private void n(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        scheduler.skip();
    }

    // TODO: Implement this
    @BotCommandHandler
    private void previous(Message message) {
        return;

//        if (!canPerformAction(message, guild.getAudioManager()))
//            return;
//
//        scheduler.playPrevious();
    }

    /**
     * @param message The message that triggered the command
     * @param duration The duration in seconds
     */
    @BotCommandHandler
    private void forward(Message message, int duration) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        forPlayingTrack(track -> track.setPosition(track.getPosition() + duration * 1000L));
    }

    /**
     * @param message The message that triggered the command
     * @param duration The duration in seconds
     */
    @BotCommandHandler
    private void back(Message message, int duration) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        forPlayingTrack(track -> track.setPosition(Math.max(0, track.getPosition() - duration * 1000L)));
    }

    @BotCommandHandler
    private void pause(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        player.setPaused(!player.isPaused());
    }

    @BotCommandHandler
    private void resume(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        player.setPaused(false);
    }

    @BotCommandHandler
    private void duration(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        forPlayingTrack(track -> message.getChannel().sendMessage("Duration is " + track.getDuration()).queue());
    }

    @BotCommandHandler
    private void seek(Message message, long position) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        forPlayingTrack(track -> track.setPosition(position));
    }

    @BotCommandHandler
    private void pos(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        forPlayingTrack(track -> messageDispatcher.sendMessage("Position is " + track.getPosition()));
    }

    @BotCommandHandler
    private void marker(final Message message, long position, final String text) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        forPlayingTrack(track ->
                track.setMarker(
                        new TrackMarker(position,
                        state ->
                                messageDispatcher.sendMessage("Trigger [" + text + "] cause [" + state.name() + "]"))));
    }

    @BotCommandHandler
    private void unmark(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        forPlayingTrack(track -> track.setMarker(null));
    }

    @BotCommandHandler
    private void song(Message message) {
        AudioTrackInfo current = player.getPlayingTrack().getInfo();

        messageDispatcher.sendMessage("Currently playing " + current.title + " by " + current.author);
    }

    @BotCommandHandler
    private void clearq(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        scheduler.clearQueue();
        messageDispatcher.sendDisposableMessage("Cleared queue.");
    }

    @BotCommandHandler
    private void shuffle(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        scheduler.shuffleQueue();
        messageDispatcher.sendDisposableMessage("Shuffled queue.");
    }

    @BotCommandHandler
    private void stop(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        // Instead of calling destroyPlayer(), we can just close the audio connection.
        // That's because when the connection is closed, the onGuildVoiceLeave event is triggered,
        // which will call destroyPlayer() for us.
        guild.getAudioManager().closeAudioConnection();
    }

    @BotCommandHandler
    private void dc(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        guild.getAudioManager().closeAudioConnection();
    }

    @BotCommandHandler
    private void leave(Message message) {
        if (!canPerformAction(message, guild.getAudioManager()))
            return;

        guild.getAudioManager().closeAudioConnection();
    }

    @BotCommandHandler
    private void setoutputchannel(Message message) {
        outputChannel.set((TextChannel) message.getChannel());
        messageDispatcher.sendDisposableMessage("Output channel set to **" + message.getChannel().getName() + "**");
    }

//    @BotCommandHandler
//    private void spotify(Message message, String url) {
//        SpotifyURLType spotifyURLType = spotify.getURLType(url);
//        if (spotifyURLType == null) {
//            messageDispatcher.sendDisposableMessage("Invalid Spotify URL.");
//            return;
//        }
//
//        AbstractDataRequest<?> request;
//
//        String id = spotify.getIdFromURL(url);
//        if (id == null) {
//            messageDispatcher.sendDisposableMessage("Could not get Spotify ID from URL.");
//            return;
//        }
//
//        switch (spotifyURLType) {
//            case Track:
//                request = spotify.getApi().getTrack(id).build();
//                try {
//                    final Track track = (Track) request.execute();
//
//                    addTrack(message, String.format("%s %s", track.getName(), track.getArtists()[0].getName()), false, false);
//                } catch (Exception e) {
//                    messageDispatcher.sendDisposableMessage("Could not get track from Spotify.");
//                }
//                break;
//            case Playlist:
//                // TODO
//                request = spotify.getApi().getPlaylist(id).build();
//                try {
//                    final Playlist playlist = (Playlist) request.execute();
//
////                    playlist.getTracks().getItems()[0].getTrack().getName();
//
//                    for (int i = 0; i < playlist.getTracks().getItems().length; i++) {
//                        final IPlaylistItem track = playlist.getTracks().getItems()[i].getTrack();
//                        addTrack(message, String.format("%s", track.getName()), false, true);
//                    }
//                } catch (Exception e) {
//                    messageDispatcher.sendDisposableMessage("Could not get playlist from Spotify.");
//                }
//                break;
//            case Album:
//                request = spotify.getApi().getAlbum(id).build();
//                try {
//                    final Album album = (Album) request.execute();
//                    System.out.println(album.getName());
//                } catch (Exception e) {
//                    messageDispatcher.sendDisposableMessage("Could not get album from Spotify.");
//                }
//                break;
//        }
//    }

    private void addTrack(final Message message, final String identifier, final boolean now, final boolean spotifyPlaylist) {
        outputChannel.set((TextChannel) message.getChannel());

        String searchQuery = identifier;

        try {
            // If it's a URL, continue.
            URL url = new URL(searchQuery);
        } catch (MalformedURLException e) {
            // Not a URL. Perform a YouTube search and only play the first result.
            searchQuery = "ytsearch: " + identifier;
        }

        Boolean isSearchQuery = (!searchQuery.equals(identifier));

        manager.loadItemOrdered(this, searchQuery, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (!connectToVoiceChannel(message, guild.getAudioManager()))
                    return;

//                String duration = String.format("%d:%02d", track.getDuration() / 60000, (track.getDuration() / 1000) % 60);

                if (!spotifyPlaylist)
                    messageDispatcher.sendDisposableMessage("Added to queue: **" + track.getInfo().title + "**");

                if (now) {
                    scheduler.playNow(track, true);
                } else {
                    scheduler.addToQueue(track);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();

                if (!isSearchQuery)
                    messageDispatcher.sendDisposableMessage("Loaded playlist: **" + playlist.getName() + "** (" + tracks.size() + ")");

                if (!connectToVoiceChannel(message, guild.getAudioManager()))
                    return;

                // If it's not a search query then normally load the playlist.
                if (!isSearchQuery) {
                    AudioTrack selected = playlist.getSelectedTrack();

                    if (selected != null) {
                        messageDispatcher.sendDisposableMessage("Selected track from playlist: **" + selected.getInfo().title + "**");
                    } else {
                        selected = tracks.get(0);
                        messageDispatcher.sendDisposableMessage("Added first track from playlist: **" + selected.getInfo().title + "**");
                    }

                    if (now) {
                        scheduler.playNow(selected, true);
                    } else {
                        scheduler.addToQueue(selected);
                    }

                    // Maximum of 1000 tracks.
                    for (int i = 0; i < Math.min(1000, playlist.getTracks().size()); i++) {
                        if (tracks.get(i) != selected) {
                            scheduler.addToQueue(tracks.get(i));
                        }
                    }
                } else {
                    // Otherwise, only play the first result from playlist.
                    AudioTrack track = playlist.getTracks().get(0);

                    messageDispatcher.sendDisposableMessage("Added to queue: **" + track.getInfo().title + "**");

                    if (now) {
                        scheduler.playNow(track, true);
                    } else {
                        scheduler.addToQueue(track);
                    }
                }
            }

            @Override
            public void noMatches() {
                messageDispatcher.sendDisposableMessage("Nothing found for " + identifier);
            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                messageDispatcher.sendMessage("Failed with message: " + throwable.getMessage() + " (" + throwable.getClass().getSimpleName() + ")");
            }
        });
    }

    private void forPlayingTrack(TrackOperation operation) {
        AudioTrack track = player.getPlayingTrack();

        if (track != null) {
            operation.execute(track);
        }
    }

    private boolean canPerformAction(final Message message, AudioManager audioManager) {
        Member member = message.getMember();
        if (member == null)
            return false;

        // Check permissions
        if (!message.getGuild().getSelfMember().hasPermission(message.getGuildChannel(), Permission.VOICE_CONNECT)) {
            messageDispatcher.sendDisposableMessage("Yeet does not have permissions to join a voice channel.");
            return false;
        }

        // Check if bot is already connected to a voice channel.
        if (audioManager.isConnected()) {
            // Allow for admins to perform actions even if they are not connected to the same voice channel.
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                // Check if bot is connected to a different voice channel from the user.
                if (audioManager.getConnectedChannel().getIdLong() != member.getVoiceState().getChannel().getIdLong()) {
                    messageDispatcher.sendDisposableMessage("Yeet is playing in another voice channel.");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean connectToVoiceChannel(final Message message, AudioManager audioManager) {
//        if (!voiceChannelChecksOK(message, audioManager))
//            return;

        Member member = message.getMember();
        if (member == null)
            return false;

        // Check if user is connected to a voice channel.
        VoiceChannel memberVoiceChannel = (VoiceChannel) member.getVoiceState().getChannel();
        if (memberVoiceChannel == null) {
            messageDispatcher.sendDisposableMessage("You are not connected to a voice channel.");
            return false;
        }

        // Join voice channel of user.
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(memberVoiceChannel);
            audioManager.setSelfDeafened(true);
        }

        return true;
    }

    private interface TrackOperation {
        void execute(AudioTrack track);
    }

    private class GlobalDispatcher implements MessageDispatcher {
        @Override
        public void sendMessage(MessageEmbed messageEmbed, Consumer<Message> success, Consumer<Throwable> failure) {
            TextChannel channel = outputChannel.get();

            if (channel != null) {
                channel.sendMessageEmbeds(messageEmbed).queue(success, failure);
            }
        }

        @Override
        public void sendMessage(String message) {
            TextChannel channel = outputChannel.get();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.CYAN);
            eb.setDescription(message);

            if (channel != null) {
                channel.sendMessageEmbeds(eb.build()).queue();
            }
        }

        @Override
        public void sendDisposableMessage(String message) {
            TextChannel channel = outputChannel.get();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.CYAN);
            eb.setDescription(message);

            channel.sendMessageEmbeds(eb.build()).queue(m -> m.delete().queueAfter(MessageDispatcher.deleteSeconds, TimeUnit.SECONDS));
        }
    }

    public static class Factory implements BotControllerFactory<MusicController> {
        @Override
        public Class<MusicController> getControllerClass() {
            return MusicController.class;
        }

        @Override
        public MusicController create(BotApplicationManager manager, BotGuildContext state, Guild guild) {
            return new MusicController(manager, state, guild);
        }
    }
}
