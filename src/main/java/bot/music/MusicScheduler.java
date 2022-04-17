package bot.music;

import bot.MessageDispatcher;
import bot.MessageType;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.ContextException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MusicScheduler extends AudioEventAdapter implements Runnable {
    private final Guild guild;
    private final AudioPlayer player;
    private final MessageDispatcher messageDispatcher;
    private final ScheduledExecutorService executorService;
    private final BlockingDeque<AudioTrack> queue;
    private final AtomicReference<Message> boxMessage;
    private final AtomicBoolean creatingBoxMessage;

    public MusicScheduler(Guild guild, AudioPlayer player, MessageDispatcher messageDispatcher, ScheduledExecutorService executorService) {
        this.guild = guild;
        this.player = player;
        this.messageDispatcher = messageDispatcher;
        this.executorService = executorService;
        this.queue = new LinkedBlockingDeque<>();
        this.boxMessage = new AtomicReference<>();
        this.creatingBoxMessage = new AtomicBoolean();

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

    // TODO: Implement this
    public void playPrevious() {
        AudioTrack previous = queue.pollFirst(); // pollFirst() returns the head element of the list

        if (previous != null) {
            if (!player.startTrack(previous, false)) {
                queue.addFirst(previous);
            }
        } else {
            player.stopTrack();

            messageDispatcher.sendDisposableMessage(MessageType.Info, "Queue finished.");
        }
    }

    public void clearQueue() {
        queue.clear();
    }

    public void shuffleQueue() {
        List<AudioTrack> q = drainQueue();
        Collections.shuffle(q);
        queue.clear();
        queue.addAll(q);
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

    private void startNextTrack(boolean noInterrupt) {
        AudioTrack next = queue.pollFirst();

        if (next != null) {
            if (!player.startTrack(next, noInterrupt)) {
                queue.addFirst(next);
            }
        } else {
            player.stopTrack();
            messageDispatcher.sendDisposableMessage(MessageType.Info, "Queue finished.");
            guild.getAudioManager().closeAudioConnection();
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        updateTrackBox(true);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
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
                    messageDispatcher.sendMessage(MessageType.TrackBox, box, created -> {
                        boxMessage.set(created);
                        creatingBoxMessage.set(false);
                    }, error -> {
                        creatingBoxMessage.set(false);
                    }, true);
                }
            }
        }
    }

    @Override
    public void run() {
        updateTrackBox(false);
    }
}
