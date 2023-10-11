package bot.records;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class ActionData {
    private final GenericInteractionCreateEvent event;
    private final InteractionHook hook;

    public ActionData(
            GenericInteractionCreateEvent event,
            InteractionHook hook) {
        this.event = event;
        this.hook = hook;
    }

    public GenericInteractionCreateEvent getEvent() {
        return this.event;
    }

    public InteractionHook getHook() {
        return this.hook;
    }
}
