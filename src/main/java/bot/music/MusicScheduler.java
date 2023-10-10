package bot.music;

import bot.listeners.BotApplicationManager;
import bot.records.InteractionResponse;
import bot.records.MessageDispatcher;
import bot.records.MessageType;
import dev.arbjerg.lavalink.protocol.v4.Track;
import dev.schlaubi.lavakord.audio.TrackEndEvent;
import dev.schlaubi.lavakord.audio.TrackStartEvent;
import dev.schlaubi.lavakord.audio.TrackStuckEvent;
import dev.schlaubi.lavakord.audio.WebSocketClosedEvent;
import dev.schlaubi.lavakord.interop.JavaPlayer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MusicScheduler implements Runnable {
    private final BotApplicationManager appManager;
    private final MusicController controller;
    private final Guild guild;
    private final JavaPlayer player;
    private final MessageDispatcher messageDispatcher;
    private final ScheduledExecutorService executorService;
    private final BlockingDeque<Track> queue;
    private final BlockingDeque<Track> history;
    private final AtomicReference<Message> boxMessage;
    private final AtomicBoolean creatingBoxMessage;
    private ScheduledFuture<?> waitingInVC;

    public MusicScheduler(BotApplicationManager appManager, Guild guild, MessageDispatcher messageDispatcher, MusicController controller) {
        this.appManager = appManager;
        this.controller = controller;
        this.guild = guild;
        this.player = appManager.getLavakord().getLink(guild.getIdLong()).getPlayer();
        this.messageDispatcher = messageDispatcher;
        this.executorService = appManager.getExecutorService();
        this.queue = new LinkedBlockingDeque<>();
        this.history = new LinkedBlockingDeque<>();
        this.boxMessage = new AtomicReference<>();
        this.creatingBoxMessage = new AtomicBoolean();

        // execute this.run() every 15 seconds
        executorService.scheduleAtFixedRate(this, 3000L, 15000L, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T o) throws Exception {
        //Serialization of object
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(o);

        //De-serialization of object
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);
        return (T) in.readObject();
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public JavaPlayer getPlayer() {
        return player;
    }

    public Guild getGuild() {
        return guild;
    }

    public BlockingDeque<Track> getQueue() {
        return queue;
    }

    public BlockingDeque<Track> getHistory() {
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
        //
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

        List<Track> q = drainQueue();
        Collections.shuffle(q);
        queue.clear();
        queue.addAll(q);

        return new InteractionResponse()
                .setSuccess(true)
                .setMessageType(MessageType.Success)
                .setMessage("Shuffled queue.");
    }

    public void addToQueue(Track audioTrack) {
        queue.addLast(audioTrack);
        startNextTrack(true);
    }

    public List<Track> drainQueue() {
        List<Track> drainedQueue = new ArrayList<>();
        queue.drainTo(drainedQueue);
        return drainedQueue;
    }

    public void playNow(Track audioTrack, boolean clearQueue) {
        if (clearQueue) {
            queue.clear();
        }

        queue.addFirst(audioTrack);
        startNextTrack(false);
    }

    public void playNext(Track audioTrack) {
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
        Track next = queue.pollFirst();

        if (next != null) {
            if (noInterrupt && player.getPlayingTrack() != null) {
                queue.addFirst(next);
            } else {
                player.playTrack(next);
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

    /*private void startNextTrack(boolean noInterrupt) {
        AudioTrack next = queue.pollFirst();

        if (next != null) {
            if (!player.playTrack(next, noInterrupt)) {
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
    }*/

    public void onTrackStartEvent(TrackStartEvent event) {
        System.out.print("Track started playing. " + event.getTrack().getInfo().getTitle());
        updateTrackBox(true);
    }

    public void onTrackEndEvent(TrackEndEvent event) {
        System.out.print("Track ended playing. " + event.getTrack().getInfo().getTitle());
        if (event.getReason().getMayStartNext()) {
            startNextTrack(true);
            messageDispatcher.sendDisposableMessage(MessageType.Info, String.format("Track **%s** finished.", event.getTrack().getInfo().getTitle()));
        }
    }

    public void onTrackStuckEvent(TrackStuckEvent event) {
        messageDispatcher.sendDisposableMessage(MessageType.Warning, String.format("Track **%s** got stuck, skipping.", event.getTrack().getInfo().getTitle()));
        startNextTrack(false);
    }

    public void onWebSocketClosedEvent(WebSocketClosedEvent event) {
        messageDispatcher.sendDisposableMessage(MessageType.Warning, "WebSocket closed, stopping player.");
        this.controller.destroyPlayer();
    }

    public void onPlayerPauseEvent() {
        updateTrackBox(false);
    }

    private void updateTrackBox(boolean newMessage) {
        Track track = player.getPlayingTrack();

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
//            MessageEmbed box = TrackBoxBuilder.buildTrackBox(50, track, player.getPaused(), player.getVolume(), queue.size());
            MessageEmbed box = TrackBoxBuilder.buildTrackBox(50, track, false, 100, queue.size());

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
