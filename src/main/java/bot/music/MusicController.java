package bot.music;

import bot.listeners.BotApplicationManager;
import bot.records.*;
import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.controller.BotCommandHandler;
import bot.controller.BotController;
import bot.controller.BotControllerFactory;
import bot.dto.Response;
import com.google.gson.Gson;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class MusicController implements BotController {
    private static final Logger log = LoggerFactory.getLogger(MusicController.class);
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

        // dc, leave
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
        if (event.getGuild() == null)
            return;

        if (!event.getGuild().getOwnerId().equals(event.getUser().getId())) {
            event
                    .getHook()
                    .setEphemeral(true)
                    .editOriginalEmbeds(
                            messageDispatcher.createEmbedMessage(MessageType.Warning, "You cannot change the prefix.").build()
                    )
                    .queue();
//            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Warning, "You cannot change the prefix.");
            return;
        }

        if (newPrefix.isEmpty() || newPrefix.length() > 2 || newPrefix.contains(" ") || newPrefix.contains("`")) {
            event
                    .getHook()
                    .setEphemeral(true)
                    .editOriginalEmbeds(
                            messageDispatcher.createEmbedMessage(MessageType.Error, "Prefix must be 1 or 2 characters long and cannot contain spaces or the character `.").build()
                    )
                    .queue();
//            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Error,
//                    "Prefix must be 1 or 2 characters long and cannot contain spaces or the character `.");
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
            event
                    .getHook()
                    .setEphemeral(true)
                    .editOriginalEmbeds(
                            messageDispatcher.createEmbedMessage(MessageType.Success, String.format("Prefix updated to `%s`.", newPrefix)).build()
                    )
                    .queue();
        } else {
//            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Error, "Failed to update prefix.");
            event
                    .getHook()
                    .setEphemeral(true)
                    .editOriginalEmbeds(
                            messageDispatcher.createEmbedMessage(MessageType.Error, "Failed to update prefix.").build()
                    )
                    .queue();
        }
    }

    @BotCommandHandler(name = "play", description = "Start playing something.", usage = "/play <name_of_track/link/playlist>")
    private void commandPlay(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(event, identifier, false, false);
    }

    @BotCommandHandler(name = "playnow", description = "Destroys current queue and plays the provided track.", usage = "/playnow <name_of_track/link/playlist>")
    private void commandPlayNow(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(event, identifier, true, false);
    }

    @BotCommandHandler(name = "playnext", description = "Adds in queue the provided track right after the current track.", usage = "/playnext <name_of_track/link/playlist>")
    private void commandPlayNext(SlashCommandInteractionEvent event, String identifier) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad, true))
            return;

        addTrack(event, identifier, false, true);
    }

    @BotCommandHandler(name = "queue", description = "Display current queue list.", usage = "/queue")
    private void commandQueue(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        // TODO: Make a better queue message embed?

        BlockingDeque<AudioTrack> _queue = scheduler.getQueue();
        if (_queue.isEmpty()) {
            event
                    .getHook()
                    .setEphemeral(true)
                    .editOriginalEmbeds(
                            messageDispatcher.createEmbedMessage(MessageType.Info, "The queue is empty.").build()
                    )
                    .queue();
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

        event
                .getHook()
                .editOriginalEmbeds(eb.build())
                .queue();
    }

    @BotCommandHandler(name = "volume", description = "Set the player volume.", usage = "/volume <0-100>")
    private void commandVolume(SlashCommandInteractionEvent event, int volume) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        if (volume > 100 || volume < 0) {
            event
                    .getHook()
                    .setEphemeral(true)
                    .editOriginalEmbeds(
                            messageDispatcher.createEmbedMessage(MessageType.Error, "Invalid volume.").build()
                    )
                    .queue();
//            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Error, "Invalid volume.");
            return;
        }

        player.setVolume(volume);

        InteractionResponse response = new InteractionResponse()
                .setSuccess(true)
                .setMessageType(MessageType.Info)
                .setMessage("Volume set to " + volume + ".");
        handleInteractionResponse(event.getHook(), response);
    }

    @BotCommandHandler(name = "skip", description = "Skip current track.", usage = "/skip")
    private void commandSkip(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.skip();
        event.getHook().deleteOriginal().queue();
    }

    // TODO: Implement 'previous' command
    @BotCommandHandler(name = "previous", description = "Play previous track.", usage = "/previous")
    private void commandPrevious(SlashCommandInteractionEvent event) {
        return;

//        if (!canPerformAction(messageDispatcher, message, guild.getAudioManager()))
//            return;
//
//        scheduler.playPrevious();
    }

    // TODO: Implement 'loop' command
    @BotCommandHandler(name = "loop", description = "Loop current track or playlist.", usage = "/loop <mode>")
    private void commandLoop(SlashCommandInteractionEvent event) {
        return;
    }

    /**
     * @param event The event that triggered the command
     * @param duration The duration in seconds
     */
    @BotCommandHandler(name = "forward", description = "Forward current track.", usage = "/forward <duration_in_seconds>")
    private void commandForward(SlashCommandInteractionEvent event, int duration) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setPosition(track.getPosition() + duration * 1000L));
        event.getHook().deleteOriginal().queue();
    }

    /**
     * @param event The event that triggered the command
     * @param duration The duration in seconds
     */
    @BotCommandHandler(name = "backward", description = "Backward current track.", usage = "/backward <duration_in_seconds>")
    private void commandBackward(SlashCommandInteractionEvent event, int duration) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> track.setPosition(Math.max(0, track.getPosition() - duration * 1000L)));
        event.getHook().deleteOriginal().queue();
    }

    @BotCommandHandler(name = "pause", description = "Pause current playing track. Use `/pause` again, or `/resume` to unpause.", usage = "/pause")
    private void commandPause(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        player.setPaused(!player.isPaused());
        event.getHook().deleteOriginal().queue();
    }

    @BotCommandHandler(name = "resume", description = "Resume currently paused track.", usage = "/resume")
    private void commandResume(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        player.setPaused(false);
        event.getHook().deleteOriginal().queue();
    }

    @BotCommandHandler(name = "song", description = "Display current playing track.", usage = "/song")
    private void commandSong(SlashCommandInteractionEvent event) {
        if (player.getPlayingTrack() == null) {
            InteractionResponse response = new InteractionResponse()
                    .setSuccess(false)
                    .setMessageType(MessageType.Warning)
                    .setMessage("Nothing is playing.");
            handleInteractionResponse(event.getHook(), response);
//            messageDispatcher.replyDisposable(event.getMessageChannel(), MessageType.Warning, "Nothing is playing.");
            return;
        }

        AudioTrackInfo current = player.getPlayingTrack().getInfo();

        InteractionResponse response = new InteractionResponse()
                .setMessageType(MessageType.Info)
                .setMessage(String.format("Currently playing **%s** by **%s**", current.title, current.author));

        handleInteractionResponse(event.getHook(), response);
//        messageDispatcher.sendMessage(MessageType.Info, "Currently playing **" + current.title + "** by **" + current.author + "**");
    }

    @BotCommandHandler(name = "clearqueue", description = "Clear the queue.", usage = "/clearqueue")
    private void commandClearQueue(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        scheduler.clearQueue();

        InteractionResponse response = new InteractionResponse()
                .setMessageType(MessageType.Success)
                .setMessage("Cleared queue.");
        handleInteractionResponse(event.getHook(), response);
    }

    @BotCommandHandler(name = "shuffle", description = "Shuffle the queue.", usage = "/shuffle")
    private void commandShuffle(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        InteractionResponse response = scheduler.shuffleQueue();
        handleInteractionResponse(event.getHook(), response);
    }

    @BotCommandHandler(name = "stop", description = "Stop the player.", usage = "/stop")
    private void commandStop(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        InteractionResponse response = scheduler.stopPlayer();
        handleInteractionResponse(event.getHook(), response);
    }

    @BotCommandHandler(name = "disconnect", description = "Disconnect the bot from the voice channel.", usage = "/dc")
    private void commandDisconnect(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
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
            return;
        }

//        if (!isOwner(event.getUser()))
//            return;

        outputChannel.set((TextChannel) event.getMessageChannel());

        InteractionResponse response = new InteractionResponse()
                .setMessageType(MessageType.Success)
                .setMessage("Output channel set to **" + event.getChannel().getName() + "**");
        handleInteractionResponse(event.getHook(), response);
    }

    @BotCommandHandler(name = "duration", description = "Display the duration of the current playing track.", usage = "/duration")
    private void commandDuration(SlashCommandInteractionEvent event) {
        ActionData ad = new ActionData(messageDispatcher, event, event.getHook(), guild.getAudioManager());
        if (!canPerformAction(ad))
            return;

        forPlayingTrack(track -> messageDispatcher.reply(event.getMessageChannel(), MessageType.Info, "Duration is " + track.getDuration()));
        event.getHook().deleteOriginal().queue();
    }

    /** ====================================
     *  End bot commands
     *  ====================================
     */

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

                event
                        .getHook()
                        .editOriginalEmbeds(
                                messageDispatcher.createEmbedMessage(
                                        MessageType.Success,
                                        String.format("Added to queue: **%s**", track.getInfo().title)
                                ).build()
                        )
                        .queue();

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
                    event
                            .getHook()
                            .editOriginalEmbeds(
                                    messageDispatcher.createEmbedMessage(
                                            MessageType.Success,
                                            String.format("Loaded playlist: **%s** (Tracks: %s)", playlist.getName(), tracks.size())
                                    ).build()
                            )
                            .queue();

                if (!connectToVoiceChannel(event, guild.getAudioManager()))
                    return;

                // If it's not a search query then normally load the playlist.
                if (!isSearchQuery) {
                    AudioTrack selected = playlist.getSelectedTrack();

                    /*if (selected != null) {
                        messageDispatcher.sendDisposableMessage(MessageType.Success, "Selected track from playlist: **" + selected.getInfo().title + "**");
                    } else {
                        selected = tracks.get(0);
                        messageDispatcher.sendDisposableMessage(MessageType.Success, "Added first track from playlist: **" + selected.getInfo().title + "**");
                    }*/

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

                    event
                            .getHook()
                            .editOriginalEmbeds(
                                    messageDispatcher.createEmbedMessage(
                                            MessageType.Success,
                                            String.format("Added to queue: **%s**", track.getInfo().title)
                                    ).build()
                            )
                            .queue();

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
                event
                        .getHook()
                        .editOriginalEmbeds(
                                messageDispatcher.createEmbedMessage(MessageType.Error, String.format("Nothing found for %s", identifier)).build()
                        )
                        .queue();
            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                event
                        .getHook()
                        .editOriginalEmbeds(
                                messageDispatcher.createEmbedMessage(
                                        MessageType.Error,
                                        String.format("Failed with message: %s (%s)", throwable.getMessage(), throwable.getClass().getSimpleName())
                                ).build()
                        )
                        .queue();
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
            actionData
                .getHook()
                .setEphemeral(true)
                .editOriginalEmbeds(
                        actionData.getMessageDispatcher().createEmbedMessage(MessageType.Error, "YEEET does not have permission to join a voice channel.").build()
                )
                .queue();
            return false;
        }

        // Check if user is connected to a voice channel. Admins can bypass.
        if (mustBeInVC && !actionData.getEvent().getMember().hasPermission(Permission.ADMINISTRATOR)) {
            if (actionData.getEvent().getMember().getVoiceState() == null)
                return false;

            VoiceChannel memberVoiceChannel = (VoiceChannel) actionData.getEvent().getMember().getVoiceState().getChannel();
            if (memberVoiceChannel == null) {
                actionData
                        .getHook()
                        .setEphemeral(true)
                        .editOriginalEmbeds(
                                actionData.getMessageDispatcher().createEmbedMessage(MessageType.Error, "You are not connected to a voice channel.").build()
                        )
                        .queue();
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
                    actionData
                            .getHook()
                            .editOriginalEmbeds(
                                    actionData.getMessageDispatcher().createEmbedMessage(MessageType.Error, "YEEET is already connected to another voice channel.").build()
                            )
                            .queue();
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
            event
                    .getHook()
                    .setEphemeral(true)
                    .editOriginalEmbeds(
                            messageDispatcher.createEmbedMessage(MessageType.Error, "You are not connected to a voice channel.").build()
                    )
                    .queue();
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

    public static void handleInteractionResponse(InteractionHook hook, InteractionResponse response) {
        if (response == null)
            return;

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(response.getMessageType().color)
                .setDescription(response.getMessage());

        hook.setEphemeral(!response.isSuccess());
        log.info("[{}] Interaction response: {}", response.isSuccess() ? "SUCCESS" : "ERROR", response.getMessage());

        if (response.isNewMessage()) {
            hook
                    .sendMessageEmbeds(embed.build())
                    .queue();
            return;
        }

        hook
                .editOriginalEmbeds(embed.build())
                .queue();
    }

    private interface TrackOperation {
        void execute(AudioTrack track);
    }

    public static void removeTrackBoxButtonClickListener(Guild guild) {
        for (Object listener : guild.getJDA().getRegisteredListeners()) {
            if (listener instanceof TrackBoxButtonClick) {
                Guild listenerGuild = ((TrackBoxButtonClick) listener).getScheduler().getGuild();

                if (listenerGuild.getIdLong() == guild.getIdLong()) {
                    log.info("[{}] Removing listener: {}", guild.getName(), listener.getClass().getSimpleName());
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

                    log.info("[{}] Added new listener: TrackBoxButtonClick", channel.getGuild().getName());
                    channel.getJDA().addEventListener(trackBoxButtonClick);
                    channel.sendMessageEmbeds(messageEmbed).setActionRow(TrackBoxBuilder.sendButtons(channel.getGuild().getId()))
                            .queue(success, failure);
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

        @Override
        public EmbedBuilder createEmbedMessage(MessageType type, String message) {
            return new EmbedBuilder().setColor(type.color).setDescription(message);
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
