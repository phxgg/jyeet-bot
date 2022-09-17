package bot.music;

import bot.MessageType;
import bot.records.ActionData;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TrackBoxButtonClick extends ListenerAdapter {
    private final MusicScheduler scheduler;

    public TrackBoxButtonClick(MusicScheduler _scheduler) {
        scheduler = _scheduler;
    }

    public MusicScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isAcknowledged())
            event.deferEdit().queue();

        ActionData ad = null;

        try {
            ad = new ActionData(
                    scheduler.getMessageDispatcher(),
                    event,
                    Objects.requireNonNull(event.getGuild()).getAudioManager()
            );
        } catch (NullPointerException e) {
            System.out.println("[TrackBoxButtonClick] audioManager is null.");
            e.printStackTrace();
        }

        if (!MusicController.canPerformAction(ad, true))
            return;

        final String previous = scheduler.getGuild().getId() + "_trackbox_previous";
        final String pause = scheduler.getGuild().getId() + "_trackbox_pause";
        final String next = scheduler.getGuild().getId() + "_trackbox_next";
        final String shuffle = scheduler.getGuild().getId() + "_trackbox_shuffle";
        final String stop = scheduler.getGuild().getId() + "_trackbox_stop";

        String buttonId = event.getButton().getId();

        // Cannot use 'switch' here because the 'case' requires a constant.
        assert buttonId != null;
        if (buttonId.equals(previous)) {
            scheduler.getMessageDispatcher().replyDisposable(ad.getEvent().getMessageChannel(), MessageType.Warning, "Under construction.");
        } else if (buttonId.equals(pause)) {
            scheduler.getPlayer().setPaused(!scheduler.getPlayer().isPaused());
        } else if (buttonId.equals(next)) {
            scheduler.skip();
        } else if (buttonId.equals(shuffle)) {
            scheduler.shuffleQueue();
        } else if (buttonId.equals(stop)) {
            scheduler.stopPlayer();
//            event.getGuild().getAudioManager().closeAudioConnection();
        }
    }
}
