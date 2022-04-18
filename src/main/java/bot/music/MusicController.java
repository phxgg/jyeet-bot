package bot.music;

import bot.*;
import bot.MessageType;
import bot.controller.BotCommandHandler;
import bot.controller.BotController;
import bot.controller.BotControllerFactory;
import bot.records.ActionData;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.remote.RemoteNode;
import com.sedmelluq.discord.lavaplayer.remote.message.NodeStatisticsMessage;
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
import java.util.Map;
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
    private final TrackBoxButtonClick trackBoxButtonClick;

    public MusicController(BotApplicationManager manager, BotGuildContext state, Guild guild) {
        this.manager = manager.getPlayerManager();
        this.guild = guild;
        this.equalizer = new EqualizerFactory();

        player = manager.getPlayerManager().createPlayer();
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        outputChannel = new AtomicReference<>();

        messageDispatcher = new GlobalDispatcher();
        scheduler = new MusicScheduler(guild, player, messageDispatcher, manager.getExecutorService());

        trackBoxButtonClick = new TrackBoxButtonClick(scheduler);

        player.addListener(scheduler);
    }

    public Guild getGuild() {
        return guild;
    }

    public void destroyPlayer() {
        scheduler.clearQueue();
        player.setVolume(100);
        player.setFilterFactory(null);
        player.destroy();
//        guild.getAudioManager().setSendingHandler(null);
        guild.getAudioManager().closeAudioConnection();
        messageDispatcher.sendDisposableMessage(MessageType.Info, "Player stopped.");
    }

    /** ====================================
     *  Start bot commands
     *  ====================================
     */

    @BotCommandHandler
    private void help(Message message) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Yeet Bot");

        eb.setColor(Color.CYAN);
        eb.setDescription("**Commands**");

        String prefix = System.getProperty("prefix");

        // help
        eb.addField(
                String.format("`%shelp`",
                        prefix),
                "You're looking at it right now dumbass.",
                false
        );

        // play, p
        eb.addField(
                String.format("`%splay <name_of_track/link/playlist>`",
                        prefix),
                String.format("Alternative: `%sp` - Start playing something.",
                        prefix),
                false);

        // stop, dc, leave
        eb.addField(
                String.format("`%sstop`",
                        prefix),
                String.format("Alternative: `%sdc`, `%sleave` - Stop player and disconnect bot from channel.",
                        prefix,
                        prefix),
                false
        );

        // pause, resume
        eb.addField(
                String.format("`%spause`",
                        prefix),
                String.format("Pause current playing song. Use `%spause` or `%sresume` to unpause.",
                        prefix,
                        prefix),
                false
        );

        // playnow
        eb.addField(
                String.format("`%splaynow <name_of_track/link/playlist>`",
                        prefix),
                "Destroys current queue and plays whatever provided.",
                false
        );

        // queue
        eb.addField(
                String.format("`%squeue`",
                        prefix),
                "Display current queue list.",
                false
        );

        // clearq
        eb.addField(
                String.format("`%sclearq`",
                        prefix),
                "Clears the queue.",
                false
        );

        // shuffle
        eb.addField(
                String.format("`%sshuffle`",
                        prefix),
                "Shuffle the queue.",
                false
        );

        // skip, next, n
        eb.addField(
                String.format("`%sskip`",
                        prefix),
                String.format("Alternatives: `%snext`, `%sn` - Skip the current song.",
                        prefix,
                        prefix),
                false
        );

        // volume
        eb.addField(
                String.format("`%svolume <0-100>`",
                        prefix),
                "Set the player volume.",
                false
        );

        // forward
        eb.addField(
                String.format("`%sforward <seconds>`",
                        prefix),
                "Forward track by given seconds.",
                false
        );

        // backward
        eb.addField(
                String.format("`%sbackward <seconds>`",
                        prefix),
                "Backward track by given seconds.",
                false
        );

        message.replyEmbeds(eb.build()).queue();
    }

    @BotCommandHandler
    private void play(Message message, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(message, identifier, false, false);
    }

    @BotCommandHandler
    private void p(Message message, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(message, identifier, false, false);
    }

    @BotCommandHandler
    private void playnow(Message message, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(message, identifier, true, false);
    }

    @BotCommandHandler
    private void playnext(Message message, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(message, identifier, false, true);
//        messageDispatcher.sendDisposableMessage(MessageType.Warning, "Under construction.");
    }

    @BotCommandHandler
    private void queue(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        // TODO: Make a better queue message embed?

        BlockingDeque<AudioTrack> _queue = scheduler.getQueue();
        if (_queue.isEmpty()) {
            messageDispatcher.sendDisposableMessage(MessageType.Info, "The queue is empty.");
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
    private void volume(Message message, int volume) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        if (volume > 100 || volume < 0) {
            messageDispatcher.sendDisposableMessage(MessageType.Error, "Invalid volume.");
            return;
        }

        player.setVolume(volume);
    }

    @BotCommandHandler
    private void skip(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.skip();
    }

    @BotCommandHandler
    private void next(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.skip();
    }

    @BotCommandHandler
    private void n(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.skip();
    }

    // TODO: Implement this
    @BotCommandHandler
    private void previous(Message message) {
        return;

//        if (!canPerformAction(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager()))
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
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setPosition(track.getPosition() + duration * 1000L));
    }

    /**
     * @param message The message that triggered the command
     * @param duration The duration in seconds
     */
    @BotCommandHandler
    private void backward(Message message, int duration) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setPosition(Math.max(0, track.getPosition() - duration * 1000L)));
    }

    @BotCommandHandler
    private void pause(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        player.setPaused(!player.isPaused());
    }

    @BotCommandHandler
    private void resume(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        player.setPaused(false);
    }

    @BotCommandHandler
    private void song(Message message) {
        outputChannel.set((TextChannel) message.getChannel());

        if (player.getPlayingTrack() == null) {
            messageDispatcher.sendDisposableMessage(MessageType.Warning, "Nothing is playing.");
            return;
        }

        AudioTrackInfo current = player.getPlayingTrack().getInfo();

        messageDispatcher.sendMessage(MessageType.Info, "Currently playing **" + current.title + "** by **" + current.author + "**");
    }

    @BotCommandHandler
    private void clearq(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        outputChannel.set((TextChannel) message.getChannel());
        scheduler.clearQueue();
        messageDispatcher.sendDisposableMessage(MessageType.Success, "Cleared queue.");
    }

    @BotCommandHandler
    private void shuffle(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        outputChannel.set((TextChannel) message.getChannel());
        scheduler.shuffleQueue();
    }

    @BotCommandHandler
    private void stop(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        // Instead of calling destroyPlayer(), we can just close the audio connection.
        // That's because when the connection is closed, the onGuildVoiceLeave event is triggered,
        // which will call destroyPlayer() for us.
        guild.getAudioManager().closeAudioConnection();
    }

    @BotCommandHandler
    private void dc(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        guild.getAudioManager().closeAudioConnection();
    }

    @BotCommandHandler
    private void leave(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        guild.getAudioManager().closeAudioConnection();
    }

    @BotCommandHandler
    private void setoutputchannel(Message message) {
        if (!isOwner(message.getAuthor()))
            return;

        outputChannel.set((TextChannel) message.getChannel());
        messageDispatcher.sendDisposableMessage(MessageType.Success, "Output channel set to **" + message.getChannel().getName() + "**");
    }

    @BotCommandHandler
    private void duration(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> messageDispatcher.sendMessage(MessageType.Info, "Duration is " + track.getDuration()));
    }

    @BotCommandHandler
    private void seek(Message message, long position) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setPosition(position));
    }

    @BotCommandHandler
    private void pos(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> messageDispatcher.sendMessage(MessageType.Info, "Position is " + track.getPosition()));
    }

    @BotCommandHandler
    private void marker(final Message message, long position, final String text) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track ->
                track.setMarker(
                        new TrackMarker(position,
                                state ->
                                        messageDispatcher.sendMessage(MessageType.Info, "Trigger [" + text + "] cause [" + state.name() + "]"))));
    }

    @BotCommandHandler
    private void unmark(Message message) {
        ActionData ad = new ActionData(messageDispatcher, message.getMember(), message.getGuild(), message.getGuildChannel(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setMarker(null));
    }

    @BotCommandHandler
    private void nodes(Message message, String addressList) {
        if (!isOwner(message.getAuthor()))
            return;

        manager.useRemoteNodes(addressList.split(" "));
    }

    @BotCommandHandler
    private void local(Message message) {
        if (!isOwner(message.getAuthor()))
            return;

        manager.useRemoteNodes();
    }

    @BotCommandHandler
    private void nodeinfo(Message message) {
        if (!isOwner(message.getAuthor()))
            return;

        for (RemoteNode node : manager.getRemoteNodeRegistry().getNodes()) {
            String report = buildReportForNode(node);
            message.getChannel().sendMessage(report).queue();
        }
    }

    @BotCommandHandler
    private void provider(Message message) {
        if (!isOwner(message.getAuthor()))
            return;

        forPlayingTrack(track -> {
            RemoteNode node = manager.getRemoteNodeRegistry().getNodeUsedForTrack(track);

            if (node != null) {
                message.getChannel().sendMessage("Node " + node.getAddress()).queue();
            } else {
                message.getChannel().sendMessage("Not played by a remote node.").queue();
            }
        });
    }

    @BotCommandHandler
    private void hex(Message message, int pageCount) {
        if (!isOwner(message.getAuthor()))
            return;

        manager.source(YoutubeAudioSourceManager.class).setPlaylistPageCount(pageCount);
    }

    @BotCommandHandler
    private void serialize(Message message) throws IOException {
        if (!isOwner(message.getAuthor()))
            return;

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
        if (!isOwner(message.getAuthor()))
            return;

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

    /** ====================================
     *  End bot commands
     *  ====================================
     */

    public static boolean isOwner(final User user) {
        String stam_user_id = "407904130690973707";
        return user.getId().equals(stam_user_id);
    }

    private String buildReportForNode(RemoteNode node) {
        StringBuilder builder = new StringBuilder();
        builder.append("--- ").append(node.getAddress()).append(" ---\n");
        builder.append("Connection state: ").append(node.getConnectionState()).append("\n");

        NodeStatisticsMessage statistics = node.getLastStatistics();
        builder.append("Node global statistics: \n").append(statistics == null ? "unavailable" : "");

        if (statistics != null) {
            builder.append("   playing tracks: ").append(statistics.playingTrackCount).append("\n");
            builder.append("   total tracks: ").append(statistics.totalTrackCount).append("\n");
            builder.append("   system CPU usage: ").append(statistics.systemCpuUsage).append("\n");
            builder.append("   process CPU usage: ").append(statistics.processCpuUsage).append("\n");
        }

        builder.append("Minimum tick interval: ").append(node.getTickMinimumInterval()).append("\n");
        builder.append("Tick history capacity: ").append(node.getTickHistoryCapacity()).append("\n");

        List<RemoteNode.Tick> ticks = node.getLastTicks(false);
        builder.append("Number of ticks in history: ").append(ticks.size()).append("\n");

        if (ticks.size() > 0) {
            int tail = Math.min(ticks.size(), 3);
            builder.append("Last ").append(tail).append(" ticks:\n");

            for (int i = ticks.size() - tail; i < ticks.size(); i++) {
                RemoteNode.Tick tick = ticks.get(i);

                builder.append("   [duration ").append(tick.endTime - tick.startTime).append("]\n");
                builder.append("   start time: ").append(tick.startTime).append("\n");
                builder.append("   end time: ").append(tick.endTime).append("\n");
                builder.append("   response code: ").append(tick.responseCode).append("\n");
                builder.append("   request size: ").append(tick.requestSize).append("\n");
                builder.append("   response size: ").append(tick.responseSize).append("\n");
            }
        }

        List<AudioTrack> tracks = node.getPlayingTracks();

        builder.append("Number of playing tracks: ").append(tracks.size()).append("\n");

        if (tracks.size() > 0) {
            int head = Math.min(tracks.size(), 3);
            builder.append("First ").append(head).append(" tracks:\n");

            for (int i = 0; i < head; i++) {
                AudioTrack track = tracks.get(i);

                builder.append("   [identifier ").append(track.getInfo().identifier).append("]\n");
                builder.append("   name: ").append(track.getInfo().author).append(" - ").append(track.getInfo().title).append("\n");
                builder.append("   progress: ").append(track.getPosition()).append(" / ").append(track.getDuration()).append("\n");
            }
        }

        builder.append("Balancer penalties: ").append(tracks.size()).append("\n");

        for (Map.Entry<String, Integer> penalty : node.getBalancerPenaltyDetails().entrySet()) {
            builder.append("   ").append(penalty.getKey()).append(": ").append(penalty.getValue()).append("\n");
        }

        return builder.toString();
    }

    private void addTrack(
            final Message message,
            final String identifier,
            final boolean playNow,
            final boolean playNext) {
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

                messageDispatcher.sendDisposableMessage(MessageType.Success, "Added to queue: **" + track.getInfo().title + "**");

                if (playNow) {
                    scheduler.playNow(track, true);
                } else if (playNext) {
                    scheduler.playNext(track);
                } else {
                    scheduler.addToQueue(track);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();

                if (!isSearchQuery)
                    messageDispatcher.sendDisposableMessage(MessageType.Success, "Loaded playlist: **" + playlist.getName() + "** (" + tracks.size() + ")");

                if (!connectToVoiceChannel(message, guild.getAudioManager()))
                    return;

                // If it's not a search query then normally load the playlist.
                if (!isSearchQuery) {
                    AudioTrack selected = playlist.getSelectedTrack();

                    if (selected != null) {
                        messageDispatcher.sendDisposableMessage(MessageType.Success, "Selected track from playlist: **" + selected.getInfo().title + "**");
                    } else {
                        selected = tracks.get(0);
                        messageDispatcher.sendDisposableMessage(MessageType.Success, "Added first track from playlist: **" + selected.getInfo().title + "**");
                    }

                    if (playNow) {
                        scheduler.playNow(selected, true);
                    } else if (playNext) {
                        scheduler.playNext(selected);
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

                    messageDispatcher.sendDisposableMessage(MessageType.Success, "Added to queue: **" + track.getInfo().title + "**");

                    if (playNow) {
                        scheduler.playNow(track, true);
                    } else if (playNext) {
                        scheduler.playNext(track);
                    } else {
                        scheduler.addToQueue(track);
                    }
                }
            }

            @Override
            public void noMatches() {
                messageDispatcher.sendDisposableMessage(MessageType.Warning, "Nothing found for " + identifier);
            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                messageDispatcher.sendMessage(MessageType.Error, "Failed with message: " + throwable.getMessage() + " (" + throwable.getClass().getSimpleName() + ")");
            }
        });
    }

    private void forPlayingTrack(TrackOperation operation) {
        AudioTrack track = player.getPlayingTrack();

        if (track != null) {
            operation.execute(track);
        }
    }

    public static boolean canPerformAction(ActionData actionData) {
        return canPerformAction(actionData, false);
    }

    public static boolean canPerformAction(ActionData actionData, final boolean mustBeInVC) {
        if (actionData == null)
            return false;

        if (actionData.getMember() == null)
            return false;

        // Check permissions
        if (!actionData.getGuild().getSelfMember().hasPermission(actionData.getGuildChannel(), Permission.VOICE_CONNECT)) {
            actionData.getMessageDispatcher().sendDisposableMessage(MessageType.Error, "Yeet does not have permissions to join a voice channel.");
            return false;
        }

        // Check if user is connected to a voice channel. Admins can bypass.
        if (mustBeInVC && !actionData.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            VoiceChannel memberVoiceChannel = (VoiceChannel) actionData.getMember().getVoiceState().getChannel();
            if (memberVoiceChannel == null) {
                actionData.getMessageDispatcher().sendDisposableMessage(MessageType.Error, "You are not connected to a voice channel.");
                return false;
            }
        }

        // Check if bot is already connected to a voice channel.
        if (actionData.getAudioManager().isConnected()) {
            // Admins can bypass.
            if (!actionData.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                // Check if bot is connected to a different voice channel from the user.
                if (actionData.getAudioManager().getConnectedChannel().getIdLong() != actionData.getMember().getVoiceState().getChannel().getIdLong()) {
                    actionData.getMessageDispatcher().sendDisposableMessage(MessageType.Error, "Yeet is playing in another voice channel.");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean connectToVoiceChannel(final Message message, AudioManager audioManager) {
        Member member = message.getMember();
        if (member == null)
            return false;

        // Check if user is connected to a voice channel.
        // We're checking this again because this is specifically for the !play (and similar) commands.
        // We're only going to play if the user is connected to a voice channel, there's no reason
        // for admins to be able to bypass this, as done in canPerformAction().
        VoiceChannel memberVoiceChannel = (VoiceChannel) member.getVoiceState().getChannel();
        if (memberVoiceChannel == null) {
            messageDispatcher.sendDisposableMessage(MessageType.Error, "You are not connected to a voice channel.");
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

    public static void removeTrackBoxButtonClickListener(Guild guild) {
        for (Object listener : guild.getJDA().getRegisteredListeners()) {
            if (listener instanceof TrackBoxButtonClick) {
                Guild listenerGuild = ((TrackBoxButtonClick) listener).getScheduler().getGuild();

                if (listenerGuild.getIdLong() == guild.getIdLong()) {
                    System.out.printf("[%s] Removed listener: %s%n", guild.getName(), listener.getClass().getSimpleName());
                    guild.getJDA().removeEventListener(listener);
                }
            }
        }
    }

    private class GlobalDispatcher implements MessageDispatcher {
        @Override
        public void sendMessage(
                MessageType type,
                MessageEmbed messageEmbed,
                Consumer<Message> success,
                Consumer<Throwable> failure,
                final boolean isTrackbox) {
            TextChannel channel = outputChannel.get();

            if (channel != null) {
                if (!isTrackbox)
                    channel.sendMessageEmbeds(messageEmbed).queue(success, failure);
                else {
                    removeTrackBoxButtonClickListener(channel.getGuild());

                    System.out.printf("[%s] Added new listener: TrackBoxButtonClick%n", channel.getGuild().getName());
                    channel.getJDA().addEventListener(trackBoxButtonClick);
                    channel.sendMessageEmbeds(messageEmbed).setActionRow(
                            TrackBoxBuilder.sendButtons(channel.getGuild().getId())
                    ).queue(success, failure);
                }
            }
        }

        @Override
        public void sendMessage(MessageType type, String message) {
            TextChannel channel = outputChannel.get();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(type.color);
            eb.setDescription(message);

            if (channel != null) {
                channel.sendMessageEmbeds(eb.build()).queue();
            }
        }

        @Override
        public void sendDisposableMessage(MessageType type, String message) {
            TextChannel channel = outputChannel.get();

            EmbedBuilder eb = new EmbedBuilder();
//            eb.setTitle(type.toString());
            eb.setColor(type.color);
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
