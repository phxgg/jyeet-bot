package bot.records;

import bot.MessageDispatcher;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.managers.AudioManager;

public class ActionData {
    private final MessageDispatcher messageDispatcher;
    private final Message message;
    private final AudioManager audioManager;

    public ActionData(
            MessageDispatcher messageDispatcher,
            Message message,
            AudioManager audioManager) {
        this.messageDispatcher = messageDispatcher;
        this.message = message;
        this.audioManager = audioManager;
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public Message getMessage() {
        return message;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }
}
