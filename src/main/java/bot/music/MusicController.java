package bot.music;

import bot.BotApplicationManager;
import bot.BotGuildContext;
import bot.MessageDispatcher;
import bot.MessageType;
import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.controller.BotCommandHandler;
import bot.controller.BotController;
import bot.controller.BotControllerFactory;
import bot.dto.Response;
import bot.records.ActionData;
import com.google.gson.Gson;
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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import net.iharder.Base64;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class MusicController implements BotController {
    private final BotGuildContext state;
    private final AudioPlayerManager manager;
    private final AudioPlayer player;
    private final AtomicReference<TextChannel> outputChannel;
    private final MusicScheduler scheduler;
    private final MessageDispatcher messageDispatcher;
    private final Guild guild;
    private final TrackBoxButtonClick trackBoxButtonClick;

    public MusicController(BotApplicationManager manager, BotGuildContext state, Guild guild) {
        this.state = state;
        this.manager = manager.getPlayerManager();
        this.guild = guild;

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

    public MusicScheduler getScheduler() {
        return scheduler;
    }

    public void destroyPlayer() {
        if (scheduler.getWaitingInVC() != null) {
            scheduler.getWaitingInVC().cancel(true);
            scheduler.setWaitingInVC(null);
        }

        scheduler.clearQueue();
        player.setPaused(false);
        player.setVolume(100);
        player.setFilterFactory(null);
        player.destroy();
//        guild.getAudioManager().setSendingHandler(null);
        guild.getAudioManager().closeAudioConnection();
        messageDispatcher.sendDisposableMessage(MessageType.Info, "Disconnected.");
        outputChannel.set(null);
    }

    /** ====================================
     *  Start bot commands
     *  ====================================
     */

    @BotCommandHandler
    private void help(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

//        eb.setTitle("Yeeet Bot");

        eb.setColor(Color.CYAN);
        eb.setDescription("**Commands**");

        eb.setAuthor("Yeeet Bot", "https://github.com/phxgg", "https://i.imgur.com/lIzJ56T.png");
        eb.setFooter("Made by phxgg", null);

//        String prefix = System.getProperty("prefix");
        String prefix = "/"; // state.guildPrefix

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
                String.format("Pause current playing track. Use `%spause` or `%sresume` to unpause.",
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

        // playnext
        eb.addField(
                String.format("`%splaynext <name_of_track/link/playlist>`",
                        prefix),
                "Adds in queue whatever provided right after the current track.",
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

        event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();
    }

    @BotCommandHandler
    private void prefix(SlashCommandInteractionEvent event, String newPrefix) {
        if (event.getGuild() == null)
            return;

        if (!event.getGuild().getOwnerId().equals(event.getUser().getId())) {
            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Warning, "You cannot change the prefix.");
            return;
        }

        if (newPrefix.isEmpty() || newPrefix.length() > 2 || newPrefix.contains(" ") || newPrefix.contains("`")) {
            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Error,
                    "Prefix must be 1 or 2 characters long and cannot contain spaces or the character `.");
            return;
        }

        Gson gson = new Gson();

        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", event.getGuild().getId());
            put("prefix", newPrefix);
        }};

        String post = WebReq.Post("/servers/updateGuildPrefix", data);
        Response r = gson.fromJson(post, Response.class);

        if (r.getCode() == StatusCodes.OK.getCode()) {
            state.guildPrefix = newPrefix;
            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Success, String.format("Prefix updated to `%s`.", newPrefix));
        } else {
            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Error, "Failed to update prefix.");
        }
    }

//    @BotCommandHandler
//    private void playlocal(SlashCommandInteractionEvent event, String identifier) throws FileNotFoundException {
//        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
//        if (!canPerformAction(ad, true))
//            return;
//
//        AudioTrack t = new Mp3AudioTrack(new AudioTrackInfo("test", "test", 232608, "", false, ""),
//                new NonSeekableInputStream(new FileInputStream(new File("C:/Users/stam/Desktop/beats/summer breakdown.mp3"))));
//
//        addLocalTrack(t);
//    }

    @BotCommandHandler
    private void play(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(event, identifier, false, false);
    }

    @BotCommandHandler
    private void p(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(event, identifier, false, false);
    }

    @BotCommandHandler
    private void playnow(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(event, identifier, true, false);
    }

    @BotCommandHandler
    private void playnext(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(event, identifier, false, true);
    }

    @BotCommandHandler
    private void queue(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        // TODO: Make a better queue message embed?

        BlockingDeque<AudioTrack> _queue = scheduler.getQueue();
        if (_queue.isEmpty()) {
            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Info, "The queue is empty.");
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Queue");
        eb.setColor(Color.CYAN);

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

        event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();
    }

    @BotCommandHandler
    private void volume(SlashCommandInteractionEvent event, int volume) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        if (volume > 100 || volume < 0) {
            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Error, "Invalid volume.");
            return;
        }

        player.setVolume(volume);
    }

    @BotCommandHandler
    private void skip(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.skip();
    }

    @BotCommandHandler
    private void next(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.skip();
    }

    @BotCommandHandler
    private void n(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.skip();
    }

    // TODO: Implement 'previous' command
    @BotCommandHandler
    private void previous(SlashCommandInteractionEvent event) {
        return;

//        if (!canPerformAction(messageDispatcher, message, guild.getAudioManager()))
//            return;
//
//        scheduler.playPrevious();
    }

    // TODO: Implement 'loop' command
    @BotCommandHandler
    private void loop(SlashCommandInteractionEvent evennt) {
        return;
    }

    /**
     * @param event The event that triggered the command
     * @param duration The duration in seconds
     */
    @BotCommandHandler
    private void forward(SlashCommandInteractionEvent event, int duration) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setPosition(track.getPosition() + duration * 1000L));
    }

    /**
     * @param event The event that triggered the command
     * @param duration The duration in seconds
     */
    @BotCommandHandler
    private void backward(SlashCommandInteractionEvent event, int duration) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setPosition(Math.max(0, track.getPosition() - duration * 1000L)));
    }

    @BotCommandHandler
    private void pause(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        player.setPaused(!player.isPaused());
    }

    @BotCommandHandler
    private void resume(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        player.setPaused(false);
    }

    @BotCommandHandler
    private void song(SlashCommandInteractionEvent event) {
        if (player.getPlayingTrack() == null) {
            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Warning, "Nothing is playing.");
            return;
        }

        AudioTrackInfo current = player.getPlayingTrack().getInfo();

        messageDispatcher.sendMessage(MessageType.Info, "Currently playing **" + current.title + "** by **" + current.author + "**");
    }

    @BotCommandHandler
    private void clearq(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.clearQueue();
        messageDispatcher.sendDisposableMessage(MessageType.Success, "Cleared queue.");
    }

    @BotCommandHandler
    private void shuffle(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.shuffleQueue();
    }

    @BotCommandHandler
    private void stop(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.stopPlayer();
    }

    @BotCommandHandler
    private void dc(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        // Instead of calling destroyPlayer(), we can just close the audio connection.
        // That's because when the connection is closed, the onGuildVoiceLeave event is triggered,
        // which will call destroyPlayer() for us.
        guild.getAudioManager().closeAudioConnection();
    }

    @BotCommandHandler
    private void leave(SlashCommandInteractionEvent event) {
//        outputChannel.set((TextChannel) message.getChannel());

        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        guild.getAudioManager().closeAudioConnection();
    }

    @BotCommandHandler
    private void setoutputchannel(SlashCommandInteractionEvent event) {
        if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

//        if (!isOwner(event.getUser()))
//            return;

        outputChannel.set((TextChannel) event.getMessageChannel());
        messageDispatcher.sendDisposableMessage(MessageType.Success, "Output channel set to **" + event.getChannel().getName() + "**");
    }

    @BotCommandHandler
    private void duration(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> messageDispatcher.reply(event.getMessageChannel(), MessageType.Info, "Duration is " + track.getDuration()));
    }

    @BotCommandHandler
    private void seek(SlashCommandInteractionEvent event, long position) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setPosition(position));
    }

    @BotCommandHandler
    private void pos(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> messageDispatcher.reply(event.getMessageChannel(), MessageType.Info, "Position is " + track.getPosition()));
    }

    @BotCommandHandler
    private void marker(final SlashCommandInteractionEvent event, long position, final String text) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track ->
                track.setMarker(
                        new TrackMarker(position,
                                state ->
                                        messageDispatcher.reply(event.getMessageChannel(), MessageType.Info, "Trigger [" + text + "] cause [" + state.name() + "]"))));
    }

    @BotCommandHandler
    private void unmark(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setMarker(null));
    }

    @BotCommandHandler
    private void nodes(SlashCommandInteractionEvent event, String addressList) {
        if (!isOwner(event.getUser()))
            return;

        manager.useRemoteNodes(addressList.split(" "));
    }

    @BotCommandHandler
    private void local(SlashCommandInteractionEvent event) {
        if (!isOwner(event.getUser()))
            return;

        manager.useRemoteNodes();
    }

    @BotCommandHandler
    private void nodeinfo(SlashCommandInteractionEvent event) {
        if (!isOwner(event.getUser()))
            return;

        for (RemoteNode node : manager.getRemoteNodeRegistry().getNodes()) {
            String report = buildReportForNode(node);
//            event.getTextChannel().sendMessage(report).queue();
            event.getMessageChannel().sendMessage(report).queue();
        }
    }

    @BotCommandHandler
    private void provider(SlashCommandInteractionEvent event) {
        if (!isOwner(event.getUser()))
            return;

        forPlayingTrack(track -> {
            RemoteNode node = manager.getRemoteNodeRegistry().getNodeUsedForTrack(track);

            if (node != null) {
                event.getMessageChannel().sendMessage("Node " + node.getAddress()).queue();
            } else {
                event.getMessageChannel().sendMessage("Not played by a remote node.").queue();
            }
        });
    }

    @BotCommandHandler
    private void hex(SlashCommandInteractionEvent event, int pageCount) {
        if (!isOwner(event.getUser()))
            return;

        manager.source(YoutubeAudioSourceManager.class).setPlaylistPageCount(pageCount);
    }

    @BotCommandHandler
    private void serialize(SlashCommandInteractionEvent event) throws IOException {
        if (!isOwner(event.getUser()))
            return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageOutput outputStream = new MessageOutput(baos);

        for (AudioTrack track : scheduler.drainQueue()) {
            manager.encodeTrack(outputStream, track);
        }

        outputStream.finish();

        event.getMessageChannel().sendMessage(Base64.encodeBytes(baos.toByteArray())).queue();
    }

    @BotCommandHandler
    private void deserialize(SlashCommandInteractionEvent event, String content) throws IOException {
        if (!isOwner(event.getUser()))
            return;

        outputChannel.set((TextChannel) event.getMessageChannel());
        connectToVoiceChannel(event, guild.getAudioManager());

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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

//    private void addLocalTrack(AudioTrack track) {
//        scheduler.addToQueue(track);
//    }

    private void addTrack(
            final SlashCommandInteractionEvent event,
            final String identifier,
            final boolean playNow,
            final boolean playNext) {
        if (outputChannel.get() == null) {
            outputChannel.set((TextChannel) event.getMessageChannel());
        }

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
                if (!connectToVoiceChannel(event, guild.getAudioManager()))
                    return;

                messageDispatcher.sendDisposableMessage(MessageType.Success, "Added to queue: **" + track.getInfo().title + "**");

                if (playNow) {
                    scheduler.playNow(track, false);
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

                if (!connectToVoiceChannel(event, guild.getAudioManager()))
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
                        scheduler.playNow(selected, false);
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
                        scheduler.playNow(track, false);
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canPerformAction(ActionData actionData) {
        return canPerformAction(actionData, false);
    }

    public static boolean canPerformAction(ActionData actionData, final boolean mustBeInVC) {
        if (actionData == null)
            return false;

        if (actionData.getEvent().getMember() == null || actionData.getEvent().getGuild() == null)
            return false;

        // Check permissions
        if (!actionData.getEvent().getGuild().getSelfMember().hasPermission(actionData.getEvent().getGuildChannel(), Permission.VOICE_CONNECT)) {
            actionData.getMessageDispatcher().replyDisposable(actionData.getEvent().getMessageChannel(), MessageType.Error, "YEEET does not have permission to join a voice channel.");
            return false;
        }

        // Check if user is connected to a voice channel. Admins can bypass.
        if (mustBeInVC && !actionData.getEvent().getMember().hasPermission(Permission.ADMINISTRATOR)) {
            if (actionData.getEvent().getMember().getVoiceState() == null)
                return false;

            VoiceChannel memberVoiceChannel = (VoiceChannel) actionData.getEvent().getMember().getVoiceState().getChannel();
            if (memberVoiceChannel == null) {
                actionData.getMessageDispatcher().replyDisposable(actionData.getEvent().getMessageChannel(), MessageType.Error, "You are not connected to a voice channel.");
                return false;
            }
        }

        // Check if bot is already connected to a voice channel.
        if (actionData.getAudioManager().isConnected()) {
            // Admins can bypass.
            if (!actionData.getEvent().getMember().hasPermission(Permission.ADMINISTRATOR)) {
                // Check if bot is connected to a different voice channel from the user.

                // Fix warnings
                if (actionData.getAudioManager().getConnectedChannel() == null
                    || actionData.getEvent().getMember().getVoiceState() == null
                    || actionData.getEvent().getMember().getVoiceState().getChannel() == null)
                    return false;

                if (actionData.getAudioManager().getConnectedChannel().getIdLong() != actionData.getEvent().getMember().getVoiceState().getChannel().getIdLong()) {
                    actionData.getMessageDispatcher().replyDisposable(actionData.getEvent().getMessageChannel(), MessageType.Error, "YEEET is are already connected to another voice channel.");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean connectToVoiceChannel(final SlashCommandInteractionEvent event, AudioManager audioManager) {
        Member member = event.getMember();
        if (member == null)
            return false;

        // Check if user is connected to a voice channel.
        // We're checking this again because this is specifically for the !play (and similar) commands.
        // We're only going to play if the user is connected to a voice channel, there's no reason
        // for admins to be able to bypass this, as done in canPerformAction().

        // Fix warnings
        if (member.getVoiceState() == null)
            return false;

        VoiceChannel memberVoiceChannel = (VoiceChannel) member.getVoiceState().getChannel();
        if (memberVoiceChannel == null) {
            messageDispatcher.sendDisposableMessage(MessageType.Error, "You are not connected to a voice channel.");
            return false;
        }

        // Join voice channel of user.
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(memberVoiceChannel);
            audioManager.setSelfDeafened(true);

            // Server deafen the bot, so it looks red instead of the classic grey self deafen color.
//            if (event.getGuild().getSelfMember().hasPermission(Permission.ADMINISTRATOR)) {
//                // This fails because we have to wait for the user to connect to the voice channel.
//                event.getGuild().getSelfMember().deafen(true);
//            }
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
            if (outputChannel.get() == null) {
                return;
            }

            TextChannel channel = outputChannel.get();

            if (channel != null) {
                if (!isTrackbox)
                    channel.sendMessageEmbeds(messageEmbed).queue(success, failure);
                else {
                    removeTrackBoxButtonClickListener(channel.getGuild());

                    System.out.printf("[%s] Added new listener: TrackBoxButtonClick%n", channel.getGuild().getName());
                    channel.getJDA().addEventListener(trackBoxButtonClick);
                    channel.sendMessageEmbeds(messageEmbed).setActionRow(TrackBoxBuilder.sendButtons(channel.getGuild().getId())
                    ).queue(success, failure);
                }
            }
        }

        @Override
        public void sendMessage(MessageType type, String message) {
            if (outputChannel.get() == null) {
                return;
            }

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
            if (outputChannel.get() == null) {
                return;
            }

            TextChannel channel = outputChannel.get();

            EmbedBuilder eb = new EmbedBuilder();
//            eb.setTitle(type.toString());
            eb.setColor(type.color);
            eb.setDescription(message);

            channel.sendMessageEmbeds(eb.build()).queue(m -> m.delete().queueAfter(MessageDispatcher.deleteSeconds, TimeUnit.SECONDS));
        }

        @Override
        public void reply(MessageChannel channel, MessageType type, String message) {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setColor(type.color);
            eb.setDescription(message);

            channel.sendMessageEmbeds(eb.build()).queue();
        }

        @Override
        public void replyDisposable(MessageChannel channel, MessageType type, String message) {
            EmbedBuilder eb = new EmbedBuilder();

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
