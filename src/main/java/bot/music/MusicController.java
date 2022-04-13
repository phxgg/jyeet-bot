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

    public MusicController(BotApplicationManager manager, BotGuildContext state, Guild guild) {
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

    @BotCommandHandler
    private void play(Message message, String identifier) {
        addTrack(message, identifier, false);
    }

    @BotCommandHandler
    private void playnow(Message message, String identifier) {
        addTrack(message, identifier, true);
    }

    @BotCommandHandler
    private void queue(Message message) {
        // TODO: Implement queue

        BlockingDeque<AudioTrack> _queue = scheduler.getQueue();
        if (_queue.isEmpty()) {
            messageDispatcher.sendDisposableMessage("The queue is empty.");
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Queue");
        builder.setColor(Color.CYAN);
        // for each track in the queue add a line to the embed
        int i = 1;
        for (AudioTrack track : _queue) {
            builder.addField(String.format("%d", i), String.format("%s", track.getInfo().title), true);
            i++;

            // Only display 10 tracks for now
            if (i >= 10) {
                break;
            }
        }

        message.getChannel().sendMessageEmbeds(builder.build()).queue();
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
        player.setVolume(volume);
    }

//    @BotCommandHandler
//    private void nodes(Message message, String addressList) {
//        manager.useRemoteNodes(addressList.split(" "));
//    }
//
//    @BotCommandHandler
//    private void local(Message message) {
//        manager.useRemoteNodes();
//    }

    @BotCommandHandler
    private void skip(Message message) {
        scheduler.skip();
    }

    @BotCommandHandler
    private void next(Message message) {
        scheduler.skip();
    }

    @BotCommandHandler
    private void n(Message message) {
        scheduler.skip();
    }

    @BotCommandHandler
    private void forward(Message message, int duration) {
        forPlayingTrack(track -> track.setPosition(track.getPosition() + duration));
    }

    @BotCommandHandler
    private void back(Message message, int duration) {
        forPlayingTrack(track -> track.setPosition(Math.max(0, track.getPosition() - duration)));
    }

    @BotCommandHandler
    private void pause(Message message) {
        player.setPaused(!player.isPaused());
    }

    @BotCommandHandler
    private void resume(Message message) {
        player.setPaused(false);
    }

    @BotCommandHandler
    private void duration(Message message) {
        forPlayingTrack(track -> message.getChannel().sendMessage("Duration is " + track.getDuration()).queue());
    }

    @BotCommandHandler
    private void seek(Message message, long position) {
        forPlayingTrack(track -> track.setPosition(position));
    }

    @BotCommandHandler
    private void pos(Message message) {
        forPlayingTrack(track -> messageDispatcher.sendMessage("Position is " + track.getPosition()));
    }

    @BotCommandHandler
    private void marker(final Message message, long position, final String text) {
        forPlayingTrack(track ->
                track.setMarker(
                        new TrackMarker(position,
                        state ->
                                messageDispatcher.sendMessage("Trigger [" + text + "] cause [" + state.name() + "]"))));
    }

    @BotCommandHandler
    private void unmark(Message message) {
        forPlayingTrack(track -> track.setMarker(null));
    }

    @BotCommandHandler
    private void song(Message message) {
        AudioTrackInfo current = player.getPlayingTrack().getInfo();

        messageDispatcher.sendMessage("Currently playing " + current.title + " by " + current.author);
    }

    @BotCommandHandler
    private void clearq(Message message) {
        scheduler.clearQueue();
        messageDispatcher.sendDisposableMessage("Cleared queue.");
    }

    @BotCommandHandler
    private void shuffle(Message message) {
        scheduler.shuffleQueue();
        messageDispatcher.sendDisposableMessage("Shuffled queue.");
    }

    @BotCommandHandler
    private void leave(Message message) {
        scheduler.clearQueue();
        player.setVolume(100);
        player.setFilterFactory(null);
        player.destroy();
//        guild.getAudioManager().setSendingHandler(null);
        guild.getAudioManager().closeAudioConnection();
    }

    @BotCommandHandler
    private void stop(Message message) {
        scheduler.clearQueue();
        player.setVolume(100);
        player.setFilterFactory(null);
        player.destroy();
//        guild.getAudioManager().setSendingHandler(null);
        guild.getAudioManager().closeAudioConnection();
    }

    private void addTrack(final Message message, final String identifier, final boolean now) {
        outputChannel.set((TextChannel) message.getChannel());

        String searchQuery = identifier;

        try {
            // If it's a URL, continue.
            URL url = new URL(searchQuery);
        } catch (MalformedURLException e) {
            // Not a URL. Perform a youtube search and only play the first result.
            searchQuery = "ytsearch: " + identifier;
        }

        Boolean isSearchQuery = (!searchQuery.equals(identifier));

        manager.loadItemOrdered(this, searchQuery, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                connectToVoiceChannel(message, guild.getAudioManager());

//                String duration = String.format("%d:%02d", track.getDuration() / 60000, (track.getDuration() / 1000) % 60);

                messageDispatcher.sendMessage("Added to queue: " + track.getInfo().title);

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
                    messageDispatcher.sendMessage("Loaded playlist: " + playlist.getName() + " (" + tracks.size() + ")");

                connectToVoiceChannel(message, guild.getAudioManager());

                // If it's not a search query then normally load the playlist.
                if (!isSearchQuery) {
                    AudioTrack selected = playlist.getSelectedTrack();

                    if (selected != null) {
                        messageDispatcher.sendMessage("Selected track from playlist: " + selected.getInfo().title);
                    } else {
                        selected = tracks.get(0);
                        messageDispatcher.sendMessage("Added first track from playlist: " + selected.getInfo().title);
                    }

                    if (now) {
                        scheduler.playNow(selected, true);
                    } else {
                        scheduler.addToQueue(selected);
                    }

                    // TODO: Delete minimum of 20 songs in the queue.
                    // Math.min 20 because we don't want to add more than 20 tracks to the queue.
                    for (int i = 0; i < Math.min(20, playlist.getTracks().size()); i++) {
                        if (tracks.get(i) != selected) {
                            scheduler.addToQueue(tracks.get(i));
                        }
                    }
                } else {
                    // Otherwise, only play the first result from playlist.
                    AudioTrack track = playlist.getTracks().get(0);

                    messageDispatcher.sendMessage("Added to queue: " + track.getInfo().title);

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

    private void connectToVoiceChannel(final Message message, AudioManager audioManager) {
        Member member = message.getMember();
        if (member == null)
            return;

        if (!message.getGuild().getSelfMember().hasPermission(message.getGuildChannel(), Permission.VOICE_CONNECT)) {
            messageDispatcher.sendDisposableMessage("I do not have permissions to join a voice channel.");
            return;
        }

        VoiceChannel connectedChannel = (VoiceChannel) message.getMember().getVoiceState().getChannel();
        if (connectedChannel == null) {
            messageDispatcher.sendDisposableMessage("You are not connected to a voice channel.");
            return;
        }

        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(connectedChannel);
            audioManager.setSelfDeafened(true);
        }
    }

    private interface TrackOperation {
        void execute(AudioTrack track);
    }

    private class GlobalDispatcher implements MessageDispatcher {
        @Override
        public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure) {
            TextChannel channel = outputChannel.get();

            if (channel != null) {
                channel.sendMessage(message).queue(success, failure);
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
