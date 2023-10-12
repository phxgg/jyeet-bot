package bot.music;

import bot.listeners.BotApplicationManager;
import bot.records.InteractionResponse;
import bot.records.MessageDispatcher;
import bot.records.MessageType;
import dev.arbjerg.lavalink.client.*;
import dev.arbjerg.lavalink.protocol.v4.Track;
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
    private final MessageDispatcher messageDispatcher;
    private final ScheduledExecutorService executorService;
    private final BlockingDeque<Track> queue;
    private final BlockingDeque<Track> history;
    private final AtomicReference<Message> boxMessage;
    private final AtomicBoolean creatingBoxMessage;
    private ScheduledFuture<?> waitingInVC;

    public MusicScheduler(BotApplicationManager appManager, MusicController controller, Guild guild, MessageDispatcher messageDispatcher) {
        this.appManager = appManager;
        this.controller = controller;
        this.guild = guild;
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

    public Link getLink() {
        return appManager.getLavalinkClient().getLink(guild.getIdLong());
    }

    public LavalinkPlayer getPlayer() {
        return getLink().getPlayer().block();
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
        if (queue.isEmpty()) {
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
        // TODO: getLink().destroyPlayer().block(); ?
        getPlayer().clearEncodedTrack().asMono().block();
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
            if (getPlayer().getTrack() == null) {
                guild.getJDA().getDirectAudioController().disconnect(guild);
                messageDispatcher.sendDisposableMessage(MessageType.Warning, "I have been inactive for 5 minutes, I guess I'm leaving...");
            }
        }, 5, TimeUnit.MINUTES);
    }

    public void startNextTrack(boolean noInterrupt) {
        Track next = queue.pollFirst();

        if (next != null) {
            if (noInterrupt && getPlayer().getTrack() != null) {
                queue.addFirst(next);
            } else {
                getLink().createOrUpdatePlayer()
                        .setEncodedTrack(next.getEncoded())
                        .asMono()
                        .subscribe((ignored) -> {
                            // A new track has just started playing.
                            // reset waitingInVC.
                            if (waitingInVC != null) {
                                waitingInVC.cancel(true);
                                waitingInVC = null;
                            }
                        });
            }
        } else {
            // TODO: getLink().destroyPlayer().block(); ?
            getPlayer().clearEncodedTrack().asMono().block();
            messageDispatcher.sendDisposableMessage(MessageType.Info, "Queue finished.");
            waitInVC();
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

    public void onTrackStartEvent(TrackStartEvent data) {
        dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.TrackStartEvent event = data.getEvent();
//        LavalinkNode node = data.getNode();
//        LavalinkPlayer p = node.getPlayer(Long.parseLong(event.getGuildId())).block();
        System.out.print("Track started playing. " + event.getTrack().getInfo().getTitle());
        updateTrackBox(true);
    }

    public void onTrackEndEvent(TrackEndEvent data) {
        dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.TrackEndEvent event = data.getEvent();
        System.out.print("Track ended playing. " + event.getTrack().getInfo().getTitle());
        getPlayer().clearEncodedTrack().asMono().block();
        if (event.getReason().getMayStartNext()) {
            startNextTrack(true);
            messageDispatcher.sendDisposableMessage(MessageType.Info, String.format("Track **%s** finished.", event.getTrack().getInfo().getTitle()));
        }
    }

    public void onTrackStuckEvent(TrackStuckEvent data) {
        dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.TrackStuckEvent event = data.getEvent();
        messageDispatcher.sendDisposableMessage(MessageType.Warning, String.format("Track **%s** got stuck, skipping.", event.getTrack().getInfo().getTitle()));
        startNextTrack(false);
    }

    public void onTrackExceptionEvent(TrackExceptionEvent data) {
        dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.TrackExceptionEvent event = data.getEvent();
        messageDispatcher.sendDisposableMessage(MessageType.Error, String.format("Exception on track **%s**:\n%s", event.getTrack().getInfo().getTitle(), event.getException().getMessage()));
    }

    public void onWebSocketClosedEvent(WebSocketClosedEvent data) {
        dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.WebSocketClosedEvent event = data.getEvent();
        messageDispatcher.sendDisposableMessage(MessageType.Error, String.format("WebSocket closed, stopping player. Reason:\n%s", event.getReason()));
        getLink().destroyPlayer().block();
    }

    public void onPlayerUpdateEvent(PlayerUpdateEvent data) {
//        dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.PlayerUpdateEvent event = data.getEvent();
//        messageDispatcher.sendDisposableMessage(MessageType.Info, String.format("Player update event:\nPing: %s", event.getState().getPing()));
//        updateTrackBox(false);
    }

    public void updateTrackBox(boolean newMessage) {
        Track track = getPlayer().getTrack();

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
            MessageEmbed box = TrackBoxBuilder.buildTrackBox(50, getPlayer(), queue.size());

            if (message != null) {
                message.editMessageEmbeds(box).queue();
            } else {
                if (creatingBoxMessage.compareAndSet(false, true)) {
                    messageDispatcher.sendTrackBoxMessage(box, created -> {
                        boxMessage.set(created);
                        creatingBoxMessage.set(false);
                    }, error -> creatingBoxMessage.set(false));
                }
            }
        }
    }

    @Override
    public void run() {
        updateTrackBox(false);
    }
}
