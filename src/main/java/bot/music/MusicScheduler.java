package bot.music;

import bot.records.InteractionResponse;
import bot.records.MessageDispatcher;
import bot.records.MessageType;
import bot.records.TrackMetadata;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MusicScheduler extends AudioEventAdapter implements Runnable {
    private final Guild guild;
    private final AudioPlayer player;
    private final MessageDispatcher messageDispatcher;
    private final ScheduledExecutorService executorService;
    private final BlockingDeque<AudioTrack> queue;
    private final BlockingDeque<AudioTrack> history;
    private final AtomicReference<Message> boxMessage;
    private final AtomicBoolean creatingBoxMessage;
    private ScheduledFuture<?> waitingInVC;

    public MusicScheduler(Guild guild, AudioPlayer player, MessageDispatcher messageDispatcher, ScheduledExecutorService executorService) {
        this.guild = guild;
        this.player = player;
        this.messageDispatcher = messageDispatcher;
        this.executorService = executorService;
        this.queue = new LinkedBlockingDeque<>();
        this.history = new LinkedBlockingDeque<>();
        this.boxMessage = new AtomicReference<>();
        this.creatingBoxMessage = new AtomicBoolean();

        // execute this.run() every 15 seconds
        executorService.scheduleAtFixedRate(this, 3000L, 15000L, TimeUnit.MILLISECONDS);
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public Guild getGuild() {
        return guild;
    }

    public BlockingDeque<AudioTrack> getQueue() {
        return queue;
    }

    public BlockingDeque<AudioTrack> getHistory() {
        return history;
    }

    public ScheduledFuture<?> getWaitingInVC() {
        return waitingInVC;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public void setWaitingInVC(ScheduledFuture<?> waitingInVC) {
        this.waitingInVC = waitingInVC;
    }

    public void playPrevious() {
        AudioTrack previous = history.pollLast(); // pollLast() returns the last element in the deque, or null if empty.

        if (previous != null) {
            // If there is a track in the history:
            // First add a cloned track of the current track into the normal queue.
            // Aftewards, change the metadata of the current track to not be added in the history when onTrackEnd() is triggered.
            AudioTrack current = player.getPlayingTrack();
            TrackMetadata currentMetadata = (TrackMetadata) current.getUserData();

            // clone current track and its metadata
            AudioTrack clonedCurrent = current.makeClone();
            TrackMetadata clonedCurrentMetadata = currentMetadata.clone();
            clonedCurrent.setUserData(clonedCurrentMetadata);

            queue.addFirst(clonedCurrent);
            currentMetadata.setAddInHistory(false);
            // set previous track metadata to be added in the history
            TrackMetadata previousMetadata = (TrackMetadata) previous.getUserData();
            previousMetadata.setAddInHistory(true);
            // start previous track
            if (!player.startTrack(previous, false)) {
                // If the track was not started, put it back in the history.
                history.addLast(previous.makeClone());
            }
        }
    }

    // TODO: Implement 'loop' command functionality
    public void loop() {
        return;
    }

    public void clearQueue() {
        queue.clear();
        history.clear();
    }

    public InteractionResponse shuffleQueue() {
        if (!(queue.size() > 0)) {
            return new InteractionResponse()
                    .setSuccess(false)
                    .setEphemeral(true)
                    .setMessageType(MessageType.Warning)
                    .setMessage("Cannot shuffle an empty queue.");
        }

        List<AudioTrack> q = drainQueue();
        Collections.shuffle(q);
        queue.clear();
        queue.addAll(q);

        return new InteractionResponse()
                .setSuccess(true)
                .setMessageType(MessageType.Success)
                .setMessage("Shuffled queue.");
    }

    public void addToQueue(AudioTrack audioTrack) {
        queue.addLast(audioTrack);
        startNextTrack(true);
    }

    public List<AudioTrack> drainQueue() {
        List<AudioTrack> drainedQueue = new ArrayList<>();
        queue.drainTo(drainedQueue);
        return drainedQueue;
    }

    public void playNow(AudioTrack audioTrack, boolean clearQueue) {
        if (clearQueue) {
            queue.clear();
        }

        queue.addFirst(audioTrack);
        startNextTrack(false);
    }

    public void playNext(AudioTrack audioTrack) {
        queue.addFirst(audioTrack);
    }

    public void skip() {
        startNextTrack(false);
    }

    public InteractionResponse stopPlayer() {
        clearQueue();
        player.stopTrack();
        updateTrackBox(false);

        waitInVC();

        return new InteractionResponse()
                .setSuccess(true)
                .setMessageType(MessageType.Warning)
                .setMessage("Player stopped.");
    }

    public void waitInVC() {
        // Wait for 5 minutes before closing the connection.

        // Review this code when canceling ScheduledFuture. It may lead to memory leaks.
        // Read here: https://stackoverflow.com/a/14423578

        // EDIT: This actually won't be a problem, since it's a tiny bit of memory that
        // will stick around for 5 minutes after a queue has finished, then destroyed.

        if (waitingInVC != null) {
            waitingInVC.cancel(true);
        }

        waitingInVC = executorService.schedule(() -> {
            if (player.getPlayingTrack() == null) {
                guild.getAudioManager().closeAudioConnection();
                messageDispatcher.sendDisposableMessage(MessageType.Warning, "I have been inactive for 5 minutes, I guess I'm leaving...");
            }
        }, 5, TimeUnit.MINUTES);
    }

    private void startNextTrack(boolean noInterrupt) {
        AudioTrack next = queue.pollFirst();

        if (next != null) {
            if (!player.startTrack(next, noInterrupt)) {
                queue.addFirst(next);
            } else {
                // A new track has just started playing.
                // reset waitingInVC.
                if (waitingInVC != null) {
                    waitingInVC.cancel(true);
                    waitingInVC = null;
                }
            }
        } else {
            player.stopTrack();
            messageDispatcher.sendDisposableMessage(MessageType.Info, "Queue finished.");

            waitInVC();

//            guild.getAudioManager().closeAudioConnection();
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        updateTrackBox(true);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        TrackMetadata metadata = (TrackMetadata) track.getUserData();
        if (metadata != null && metadata.shouldAddInHistory()) {
            AudioTrack clonedTrack = track.makeClone();
            clonedTrack.setUserData(new TrackMetadata().setRequestedBy(metadata.getRequestedBy()));
            history.addLast(clonedTrack);
        }

        if (endReason.mayStartNext) {
            startNextTrack(true);
            messageDispatcher.sendDisposableMessage(MessageType.Info, String.format("Track **%s** finished.", track.getInfo().title));
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        messageDispatcher.sendDisposableMessage(MessageType.Warning, String.format("Track **%s** got stuck, skipping.", track.getInfo().title));

        startNextTrack(false);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        updateTrackBox(false);
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        updateTrackBox(false);
    }

    private void updateTrackBox(boolean newMessage) {
        AudioTrack track = player.getPlayingTrack();

        if (track == null || newMessage) {
            Message oldMessage = boxMessage.getAndSet(null);

            if (oldMessage != null) {
                // Will throw an exception if message has already been deleted by a user.
                // Just ignore.
                oldMessage.delete().queue();
            }
        }

        if (track != null) {
            Message message = boxMessage.get();
            MessageEmbed box = TrackBoxBuilder.buildTrackBox(50, track, player.isPaused(), player.getVolume(), queue.size());

            if (message != null) {
                message.editMessageEmbeds(box).queue();
            } else {
                if (creatingBoxMessage.compareAndSet(false, true)) {
                    messageDispatcher.sendTrackBoxMessage(box, created -> {
                        boxMessage.set(created);
                        creatingBoxMessage.set(false);
                    }, error -> {
                        creatingBoxMessage.set(false);
                    });
                }
            }
        }
    }

    @Override
    public void run() {
        updateTrackBox(false);
    }
}
