package bot.music;

import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.controller.BotCommandHandler;
import bot.controller.IBotController;
import bot.controller.IBotControllerFactory;
import bot.api.entities.Response;
import bot.listeners.BotApplicationManager;
import bot.records.*;
import com.google.gson.Gson;
import dev.arbjerg.lavalink.client.*;
import dev.arbjerg.lavalink.protocol.v4.*;
import dev.arbjerg.lavalink.protocol.v4.Exception;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingDeque;

public class MusicController implements IBotController {
    private static final Logger log = LoggerFactory.getLogger(MusicController.class);
    private final BotApplicationManager appManager;
    private final BotGuildContext state;
    private final MusicScheduler scheduler;
    private final MessageDispatcher messageDispatcher;
    private final Guild guild;

    public MusicController(BotApplicationManager appManager, BotGuildContext state, Guild guild) {
        this.appManager = appManager;
        this.state = state;
        this.guild = guild;

        this.messageDispatcher = new MessageDispatcher();
        this.scheduler = new MusicScheduler(appManager, this, guild, this.messageDispatcher);
    }

    public Guild getGuild() {
        return this.guild;
    }

    public Link getLink() {
        return this.appManager.getLavalinkClient().getLink(guild.getIdLong());
    }

    public MusicScheduler getScheduler() {
        return this.scheduler;
    }

    public MessageDispatcher getMessageDispatcher() {
        return this.messageDispatcher;
    }

    public void destroyPlayer() {
        if (scheduler.getWaitingInVC() != null) {
            scheduler.getWaitingInVC().cancel(true);
            scheduler.setWaitingInVC(null);
        }

        scheduler.clearQueue();
        getLink().destroyPlayer().block();
        guild.getJDA().getDirectAudioController().disconnect(guild);
        messageDispatcher.sendDisposableMessage(MessageType.Info, "Disconnected.");
        messageDispatcher.setOutputChannel(null);
    }

    /**
     * ====================================
     * Start bot commands
     * ====================================
     */

    @BotCommandHandler(name = "help", description = "Shows you all the available commands.", usage = "/help")
    private void commandHelp(SlashCommandInteractionEvent event) {
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

        // play
        eb.addField(
                String.format("`%splay <name_of_track/link/playlist>`",
                        prefix),
                "Start playing something.",
                false);

        // stop
        eb.addField(
                String.format("`%sstop`",
                        prefix),
                "Stop the player and clear queue.",
                false
        );

        // disconnect
        eb.addField(
                String.format("`%sdisconnect`",
                        prefix),
                "Stop the player and disconnect bot from voice call.",
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
                "Destroys current queue and plays the provided track.",
                false
        );

        // playnext
        eb.addField(
                String.format("`%splaynext <name_of_track/link/playlist>`",
                        prefix),
                "Adds in queue the provided track right after the current track.",
                false
        );

        // queue
        eb.addField(
                String.format("`%squeue`",
                        prefix),
                "Display current queue list.",
                false
        );

        // clearqueue
        eb.addField(
                String.format("`%sclearqueue`",
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

        // skip
        eb.addField(
                String.format("`%sskip`",
                        prefix),
                "Skip the current track.",
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

        event.getHook().editOriginalEmbeds(eb.build()).queue();
//        event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();
    }

    @BotCommandHandler(name = "prefix", description = "Set the prefix for this guild.", usage = "/prefix <new_prefix>")
    private void commandPrefix(SlashCommandInteractionEvent event, String newPrefix) {
        if (event.getGuild() == null) {
            event.getHook().deleteOriginal().queue();
            return;
        }

        if (!event.getGuild().getOwnerId().equals(event.getUser().getId())) {
            InteractionResponse response = new InteractionResponse()
                    .setEphemeral(true)
                    .setSuccess(false)
                    .setMessageType(MessageType.Warning)
                    .setMessage("You cannot change the prefix.");
            InteractionResponse.handle(event.getHook(), response);
            return;
        }

        if (newPrefix.isEmpty() || newPrefix.length() > 2 || newPrefix.contains(" ") || newPrefix.contains("`")) {
            InteractionResponse response = new InteractionResponse()
                    .setEphemeral(true)
                    .setSuccess(false)
                    .setMessageType(MessageType.Error)
                    .setMessage("Prefix must be 1 or 2 characters long and cannot contain spaces or the character `.");
            InteractionResponse.handle(event.getHook(), response);
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
            state.setGuildPrefix(newPrefix);
            InteractionResponse response = new InteractionResponse()
                    .setEphemeral(true)
                    .setSuccess(true)
                    .setMessageType(MessageType.Success)
                    .setMessage(String.format("Prefix updated to `%s`.", newPrefix));
            InteractionResponse.handle(event.getHook(), response);
        } else {
//            this.messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Error, "Failed to update prefix.");
            InteractionResponse response = new InteractionResponse()
                    .setEphemeral(true)
                    .setSuccess(false)
                    .setMessageType(MessageType.Error)
                    .setMessage("Failed to update prefix.");
            InteractionResponse.handle(event.getHook(), response);
        }
    }

    @BotCommandHandler(name = "play", description = "Start playing something.", usage = "/play <name_of_track/link/playlist>")
    private void commandPlay(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad, true))
            return;

        addTrackLavalink(event, identifier, false, false);
        //addTrack(event, identifier, false, false);
    }

    @BotCommandHandler(name = "playnow", description = "Destroys current queue and plays the provided track.", usage = "/playnow <name_of_track/link/playlist>")
    private void commandPlayNow(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad, true))
            return;

        addTrackLavalink(event, identifier, true, false);
    }

    @BotCommandHandler(name = "playnext", description = "Adds in queue the provided track right after the current track.", usage = "/playnext <name_of_track/link/playlist>")
    private void commandPlayNext(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad, true))
            return;

        addTrackLavalink(event, identifier, false, true);
    }

    @BotCommandHandler(name = "queue", description = "Display current queue list.", usage = "/queue")
    private void commandQueue(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        // TODO: Make a better queue message embed?

        BlockingDeque<Track> _queue = scheduler.getQueue();
        if (_queue.isEmpty()) {
            InteractionResponse response = new InteractionResponse()
                    .setSuccess(false)
                    .setMessageType(MessageType.Info)
                    .setMessage("The queue is empty.");
            InteractionResponse.handle(event.getHook(), response);
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Queue");
        eb.setColor(Color.CYAN);

        int i = 1;
        for (Track track : _queue) {
            eb.addField(String.format("%d", i), String.format("%s", track.getInfo().getTitle()), true);
            i++;

            // Only display 10 tracks for now
            if (i > 10) {
                break;
            }
        }

        if (_queue.size() > i) {
            eb.setFooter(String.format("and %d more...", _queue.size() - i), null);
        }

        event
                .getHook()
                .editOriginalEmbeds(eb.build())
                .queue();
    }

    @BotCommandHandler(name = "volume", description = "Set the player volume.", usage = "/volume <0-100>")
    private void commandVolume(SlashCommandInteractionEvent event, int volume) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        if (volume > 100 || volume < 0) {
            InteractionResponse response = new InteractionResponse()
                    .setEphemeral(true)
                    .setSuccess(false)
                    .setMessageType(MessageType.Error)
                    .setMessage("Invalid volume.");
            InteractionResponse.handle(event.getHook(), response);
            return;
        }

        getLink()
                .getPlayer()
                .flatMap(player -> player.setVolume(volume).asMono())
                .subscribe((ignored) -> {
                    InteractionResponse response = new InteractionResponse()
                            .setSuccess(true)
                            .setMessageType(MessageType.Info)
                            .setMessage("Volume set to " + volume + ".");
                    InteractionResponse.handle(event.getHook(), response);
                });
    }

    @BotCommandHandler(name = "skip", description = "Skip current track.", usage = "/skip")
    private void commandSkip(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        scheduler.skip();
        event.getHook().deleteOriginal().queue();
    }

    @BotCommandHandler(name = "previous", description = "Play previous track.", usage = "/previous")
    private void commandPrevious(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        scheduler.playPrevious();
        event.getHook().deleteOriginal().queue();
    }

    @BotCommandHandler(name = "history", description = "Shows track history.", usage = "/history")
    private void commandHistory(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        BlockingDeque<Track> _history = scheduler.getHistory();
        if (_history.isEmpty()) {
            InteractionResponse response = new InteractionResponse()
                    .setSuccess(false)
                    .setMessageType(MessageType.Info)
                    .setMessage("The history is empty.");
            InteractionResponse.handle(event.getHook(), response);
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("History");
        eb.setColor(Color.CYAN);

        // loop through history in reverse order
        List<Track> historyReversed = new ArrayList<>(_history);
        Collections.reverse(historyReversed);

        int i = 1;
        for (Track track : historyReversed) {
            eb.addField(String.format("%d", i), String.format("%s", track.getInfo().getTitle()), true);
            i++;

            // Only display 10 tracks for now
            if (i > 10) {
                break;
            }
        }

        if (_history.size() > i) {
            eb.setFooter(String.format("and %d more...", _history.size() - i), null);
        }

        event
                .getHook()
                .editOriginalEmbeds(eb.build())
                .queue();
    }

    // TODO: Implement 'loop' command
    @BotCommandHandler(name = "loop", description = "Loop current track or playlist.", usage = "/loop <mode>")
    private void commandLoop(SlashCommandInteractionEvent event) {
        return;
    }

    /**
     * @param event    The event that triggered the command
     * @param duration The duration in seconds
     */
    @BotCommandHandler(name = "forward", description = "Forward current track.", usage = "/forward <duration_in_seconds>")
    private void commandForward(SlashCommandInteractionEvent event, int duration) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        getLink().getPlayer()
                .flatMap(player -> player.setPosition(player.getPosition() + duration * 1000L).asMono())
                .subscribe(player -> {
                    InteractionResponse response = new InteractionResponse()
                            .setSuccess(true)
                            .setMessageType(MessageType.Info)
                            .setMessage("Track forwarded by " + duration + " seconds.");
                    InteractionResponse.handle(event.getHook(), response);
                });
    }

    /**
     * @param event    The event that triggered the command
     * @param duration The duration in seconds
     */
    @BotCommandHandler(name = "backward", description = "Backward current track.", usage = "/backward <duration_in_seconds>")
    private void commandBackward(SlashCommandInteractionEvent event, int duration) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        getLink().getPlayer()
                .flatMap(player -> player.setPosition(Math.max(0, player.getPosition() - duration * 1000L)).asMono())
                .subscribe(player -> {
                    InteractionResponse response = new InteractionResponse()
                            .setSuccess(true)
                            .setMessageType(MessageType.Info)
                            .setMessage("Track backwarded by " + duration + " seconds.");
                    InteractionResponse.handle(event.getHook(), response);
                });
    }

    @BotCommandHandler(name = "pause", description = "Pause current playing track. Use `/pause` again, or `/resume` to unpause.", usage = "/pause")
    private void commandPause(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        getLink()
                .getPlayer()
                .flatMap(player -> player.setPaused(!player.getPaused()).asMono())
                .subscribe(player -> event.getHook().deleteOriginal().queue());
    }

    @BotCommandHandler(name = "resume", description = "Resume currently paused track.", usage = "/resume")
    private void commandResume(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        getLink()
                .getPlayer()
                .flatMap(player -> player.setPaused(false).asMono())
                .subscribe(player -> event.getHook().deleteOriginal().queue());
    }

    @BotCommandHandler(name = "song", description = "Display current playing track.", usage = "/song")
    private void commandSong(SlashCommandInteractionEvent event) {
        getLink().getPlayer().subscribe(player -> {
            Track current = player.getTrack();
            if (current == null) {
                InteractionResponse response = new InteractionResponse()
                        .setSuccess(false)
                        .setMessageType(MessageType.Warning)
                        .setMessage("Nothing is playing.");
                InteractionResponse.handle(event.getHook(), response);
                return;
            }

            TrackInfo trackInfo = current.getInfo();
            InteractionResponse response = new InteractionResponse()
                    .setMessageType(MessageType.Info)
                    .setMessage(String.format("Currently playing **[%s](%s)** by **%s**", trackInfo.getTitle(), trackInfo.getUri(), trackInfo.getAuthor()));
            InteractionResponse.handle(event.getHook(), response);
        });
    }

    @BotCommandHandler(name = "clearqueue", description = "Clear the queue.", usage = "/clearqueue")
    private void commandClearQueue(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;
        scheduler.clearQueue();
        InteractionResponse response = new InteractionResponse()
                .setMessageType(MessageType.Success)
                .setMessage("Cleared queue.");
        InteractionResponse.handle(event.getHook(), response);
    }

    @BotCommandHandler(name = "shuffle", description = "Shuffle the queue.", usage = "/shuffle")
    private void commandShuffle(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        InteractionResponse response = scheduler.shuffleQueue();
        InteractionResponse.handle(event.getHook(), response);
    }

    @BotCommandHandler(name = "stop", description = "Stop the player.", usage = "/stop")
    private void commandStop(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;
        scheduler.stopPlayer();
        InteractionResponse response = new InteractionResponse()
                .setMessageType(MessageType.Success)
                .setMessage("Stopped player.");
        InteractionResponse.handle(event.getHook(), response);
    }

    @BotCommandHandler(name = "disconnect", description = "Disconnect the bot from the voice channel.", usage = "/dc")
    private void commandDisconnect(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        // Instead of calling destroyPlayer(), we can just close the audio connection.
        // That's because when the connection is closed, the onGuildVoiceLeave event is triggered,
        // which will call destroyPlayer() for us.
        guild.getAudioManager().closeAudioConnection();
        event.getHook().deleteOriginal().queue();
    }

    @BotCommandHandler(name = "setoutputchannel", description = "Set the output channel for the bot.", usage = "/setoutputchannel <channel>")
    private void commandSetOutputChannel(SlashCommandInteractionEvent event) {
        if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().deleteOriginal().queue();
            return;
        }

//        if (!isOwner(event.getUser()))
//            return;

        messageDispatcher.setOutputChannel((TextChannel) event.getMessageChannel());

        InteractionResponse response = new InteractionResponse()
                .setMessageType(MessageType.Success)
                .setMessage("Output channel set to **" + event.getChannel().getName() + "**");
        InteractionResponse.handle(event.getHook(), response);
    }

    @BotCommandHandler(name = "duration", description = "Display the duration of the current playing track.", usage = "/duration")
    private void commandDuration(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(event, event.getHook());
        if (!canPerformAction(ad))
            return;

        getLink().getPlayer().subscribe(player -> {
            Track current = player.getTrack();
            if (current != null) {
                String duration = TrackBoxBuilder.formatTiming(current.getInfo().getLength(), current.getInfo().getLength());
                InteractionResponse response = new InteractionResponse()
                        .setMessageType(MessageType.Info)
                        .setMessage("Duration is " + duration);
                InteractionResponse.handle(event.getHook(), response);
            }
        });
    }

    /**
     * ====================================
     * End bot commands
     * ====================================
     */

    private void addTrackLavalink(
            final SlashCommandInteractionEvent event,
            final String identifier,
            final boolean playNow,
            final boolean playNext) {
        if (messageDispatcher.getOutputChannel() == null) {
            messageDispatcher.setOutputChannel((TextChannel) event.getMessageChannel());
        }

        String searchQuery = identifier;

        try {
            // If it's a URL, continue.
            new URL(searchQuery);
        } catch (MalformedURLException e) {
            // Not a URL. Perform a YouTube search and only play the first result.
            searchQuery = "ytsearch: " + identifier;
        }

        boolean isSearchQuery = (!searchQuery.equals(identifier));

        getLink().loadItem(searchQuery).subscribe(result -> {
            if (result instanceof LoadResult.TrackLoaded trackLoaded) {
                final var track = trackLoaded.getData();

                if (!connectToVoiceChannel(event))
                    return;

                // add track metadata
//                track.setUserData(new TrackMetadata().setRequestedBy(event.getUser()));

                InteractionResponse response = new InteractionResponse()
                        .setMessageType(MessageType.Success)
                        .setMessage(String.format("Added to queue: **[%s](%s)**", track.getInfo().getTitle(), track.getInfo().getUri()));
                InteractionResponse.handle(event.getHook(), response);

                if (playNow) {
                    scheduler.playNow(track, false);
                } else if (playNext) {
                    scheduler.playNext(track);
                } else {
                    scheduler.addToQueue(track, true);
                }
            } else if (result instanceof LoadResult.PlaylistLoaded playlistLoaded) {
                final var playlist = playlistLoaded.getData();
                List<Track> tracks = playlist.getTracks();

                if (!isSearchQuery) {
                    InteractionResponse response = new InteractionResponse()
                            .setMessageType(MessageType.Success)
                            .setMessage(String.format("Loaded playlist: **%s** (Tracks: %s)", playlist.getInfo().getName(), tracks.size()));
                    InteractionResponse.handle(event.getHook(), response);
                }

                if (!connectToVoiceChannel(event))
                    return;

                // add track metadata
//                playlist.getTracks().forEach(track -> track.setUserData(new TrackMetadata().setRequestedBy(event.getUser())));

                // If it's not a search query then normally load the playlist.
                if (!isSearchQuery) {
                    Track selected = tracks.get(0);

                    if (playNow) {
                        scheduler.playNow(selected, false);
                    } else if (playNext) {
                        scheduler.playNext(selected);
                    } else {
                        scheduler.addToQueue(selected, true);
                    }

                    // Maximum of 1000 tracks.
                    for (int i = 0; i < Math.min(1000, playlist.getTracks().size()); i++) {
                        if (tracks.get(i) != selected) {
                            System.out.println("Adding to queue: " + tracks.get(i).getInfo().getTitle());
                            scheduler.addToQueue(tracks.get(i), false);
                        }
                    }
                }
            } else if (result instanceof LoadResult.SearchResult searchResult) {
                final var searchResultData = searchResult.getData();

                if (!connectToVoiceChannel(event))
                    return;

                // Only play the first result from playlist.
                Track track = searchResultData.getTracks().get(0);

                InteractionResponse response = new InteractionResponse()
                        .setMessageType(MessageType.Success)
                        .setMessage(String.format("Added to queue: **[%s](%s)**", track.getInfo().getTitle(), track.getInfo().getUri()));
                InteractionResponse.handle(event.getHook(), response);

                if (playNow) {
                    scheduler.playNow(track, false);
                } else if (playNext) {
                    scheduler.playNext(track);
                } else {
                    scheduler.addToQueue(track, true);
                }
            } else if (result instanceof LoadResult.NoMatches) {
                InteractionResponse response = new InteractionResponse()
                        .setSuccess(false)
                        .setMessageType(MessageType.Error)
                        .setMessage(String.format("Nothing found for %s", identifier));
                InteractionResponse.handle(event.getHook(), response);
            } else if (result instanceof LoadResult.LoadFailed loadFailed) {
                Exception throwable = loadFailed.getData();
                InteractionResponse response = new InteractionResponse()
                        .setSuccess(false)
                        .setMessageType(MessageType.Error)
                        .setMessage(String.format("Failed with message: %s (%s)", throwable.getMessage(), throwable.getClass().getSimpleName()));
                InteractionResponse.handle(event.getHook(), response);
            } else {
                throw new IllegalStateException("Unexpected value: " + result);
            }
        });
    }

    /*private void addTrack(
            final SlashCommandInteractionEvent event,
            final String identifier,
            final boolean playNow,
            final boolean playNext) {
        if (messageDispatcher.getOutputChannel() == null) {
            messageDispatcher.setOutputChannel((TextChannel) event.getMessageChannel());
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

        playerManager.loadItemOrdered(this, searchQuery, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (!connectToVoiceChannel(event, guild.getAudioManager()))
                    return;

                // add track metadata
                track.setUserData(new TrackMetadata().setRequestedBy(event.getUser()));

                InteractionResponse response = new InteractionResponse()
                        .setMessageType(MessageType.Success)
                        .setMessage(String.format("Added to queue: **%s**", track.getInfo().title));
                InteractionResponse.handle(event.getHook(), response);

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

                if (!isSearchQuery) {
                    InteractionResponse response = new InteractionResponse()
                            .setMessageType(MessageType.Success)
                            .setMessage(String.format("Loaded playlist: **%s** (Tracks: %s)", playlist.getName(), tracks.size()));
                    InteractionResponse.handle(event.getHook(), response);
                }

                if (!connectToVoiceChannel(event, guild.getAudioManager()))
                    return;

                // add track metadata
                playlist.getTracks().forEach(track -> track.setUserData(new TrackMetadata().setRequestedBy(event.getUser())));

                // If it's not a search query then normally load the playlist.
                if (!isSearchQuery) {
                    AudioTrack selected = playlist.getSelectedTrack();

//                    if (selected != null) {
//                        messageDispatcher.sendDisposableMessage(MessageType.Success, "Selected track from playlist: **" + selected.getInfo().title + "**");
//                    } else {
//                        selected = tracks.get(0);
//                        messageDispatcher.sendDisposableMessage(MessageType.Success, "Added first track from playlist: **" + selected.getInfo().title + "**");
//                    }

                    if (selected == null) {
                        selected = tracks.get(0);
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

                    InteractionResponse response = new InteractionResponse()
                            .setMessageType(MessageType.Success)
                            .setMessage(String.format("Added to queue: **%s**", track.getInfo().title));
                    InteractionResponse.handle(event.getHook(), response);

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
                InteractionResponse response = new InteractionResponse()
                        .setSuccess(false)
                        .setMessageType(MessageType.Error)
                        .setMessage(String.format("Nothing found for %s", identifier));
                InteractionResponse.handle(event.getHook(), response);
            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                InteractionResponse response = new InteractionResponse()
                        .setSuccess(false)
                        .setMessageType(MessageType.Error)
                        .setMessage(String.format("Failed with message: %s (%s)", throwable.getMessage(), throwable.getClass().getSimpleName()));
                InteractionResponse.handle(event.getHook(), response);
            }
        });
    }*/

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canPerformAction(ActionData actionData) {
        return canPerformAction(actionData, false);
    }

    public static boolean canPerformAction(ActionData actionData, final boolean mustBeInVC) {
        if (actionData == null)
            return false;

        GenericInteractionCreateEvent event = actionData.getEvent();
        InteractionHook hook = actionData.getHook();

        if (event.getMember() == null || event.getGuild() == null) {
            hook.deleteOriginal().queue();
            return false;
        }

        // Check permissions
        if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.VOICE_CONNECT)) {
            InteractionResponse response = new InteractionResponse()
                    .setEphemeral(true)
                    .setSuccess(false)
                    .setMessageType(MessageType.Error)
                    .setMessage("YEEET does not have permission to join a voice channel.");
            InteractionResponse.handle(hook, response);
            return false;
        }

        // Check if user is connected to a voice channel. Admins can bypass.
        if (mustBeInVC && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            if (event.getMember().getVoiceState() == null)
                return false;

            AudioChannel memberVoiceChannel = event.getMember().getVoiceState().getChannel();
            if (memberVoiceChannel == null) {
                InteractionResponse response = new InteractionResponse()
                        .setEphemeral(true)
                        .setSuccess(false)
                        .setMessageType(MessageType.Error)
                        .setMessage("You are not connected to a voice channel.");
                InteractionResponse.handle(hook, response);
                return false;
            }
        }

        Member selfMember = event.getGuild().getSelfMember();
        GuildVoiceState selfVoiceState = selfMember.getVoiceState();
        // Check if bot is already connected to a voice channel.
        if (selfVoiceState != null && selfVoiceState.inAudioChannel()) {
            // Admins can bypass.
            if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                // Check if bot is connected to a different voice channel from the user.

                // Fix warnings
                if (selfVoiceState.getChannel() == null
                        || event.getMember().getVoiceState() == null
                        || event.getMember().getVoiceState().getChannel() == null)
                    return false;

                if (selfVoiceState.getChannel().getIdLong() != event.getMember().getVoiceState().getChannel().getIdLong()) {
                    InteractionResponse response = new InteractionResponse()
                            .setSuccess(false)
                            .setMessageType(MessageType.Error)
                            .setMessage("YEEET is already connected to another voice channel.");
                    InteractionResponse.handle(hook, response);
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean connectToVoiceChannel(final SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null)
            return false;

        // Check if user is connected to a voice channel.
        // We're checking this again because this is specifically for the !play (and similar) commands.
        // We're only going to play if the user is connected to a voice channel, there's no reason
        // for admins to be able to bypass this, as done in canPerformAction().

        final GuildVoiceState memberVoiceState = member.getVoiceState();
        // Fix warnings
        if (memberVoiceState == null)
            return false;

        if (!memberVoiceState.inAudioChannel() || memberVoiceState.getChannel() == null) {
            InteractionResponse response = new InteractionResponse()
                    .setEphemeral(true)
                    .setSuccess(false)
                    .setMessageType(MessageType.Error)
                    .setMessage("You are not connected to a voice channel.");
            InteractionResponse.handle(event.getHook(), response);
            return false;
        }

        // Join voice channel of user.
        event.getJDA().getDirectAudioController().connect(memberVoiceState.getChannel());

        return true;
    }

    public static class Factory implements IBotControllerFactory<MusicController> {
        @Override
        public Class<MusicController> getControllerClass() {
            return MusicController.class;
        }

        @Override
        public MusicController create(BotApplicationManager appManager, BotGuildContext state, Guild guild) {
            return new MusicController(appManager, state, guild);
        }
    }
}
