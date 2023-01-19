package bot.records;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.managers.AudioManager;

public class ActionData {
    private final MessageDispatcher messageDispatcher;
    private final GenericInteractionCreateEvent event;
    private final AudioManager audioManager;

    public ActionData(
            MessageDispatcher messageDispatcher,
            GenericInteractionCreateEvent event,
            AudioManager audioManager) {
        this.messageDispatcher = messageDispatcher;
        this.event = event;
        this.audioManager = audioManager;
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public GenericInteractionCreateEvent getEvent() {
        return event;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }
}
