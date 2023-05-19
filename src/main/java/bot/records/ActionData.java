package bot.records;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;

public class ActionData {
    private final GenericInteractionCreateEvent event;
    private final InteractionHook hook;
    private final AudioManager audioManager;

    public ActionData(
            GenericInteractionCreateEvent event,
            InteractionHook hook,
            AudioManager audioManager) {
        this.event = event;
        this.hook = hook;
        this.audioManager = audioManager;
    }

    public GenericInteractionCreateEvent getEvent() {
        return this.event;
    }

    public InteractionHook getHook() {
        return this.hook;
    }

    public AudioManager getAudioManager() {
        return this.audioManager;
    }
}
