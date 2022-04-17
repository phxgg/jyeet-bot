package bot.music;

import bot.ActionData;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

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

        ActionData ad = new ActionData(scheduler.getMessageDispatcher(), event.getMember(), event.getGuild(), event.getGuildChannel(), event.getGuild().getAudioManager());
        if (!MusicController.canPerformAction(ad))
            return;

        final String previous = scheduler.getGuild().getId() + "_trackbox_previous";
        final String pause = scheduler.getGuild().getId() + "_trackbox_pause";
        final String next = scheduler.getGuild().getId() + "_trackbox_next";

        String buttonId = event.getButton().getId();

        assert buttonId != null;
        if (buttonId.equals(previous)) {
            event.getChannel().sendMessage("Back button clicked").queue();
        } else if (buttonId.equals(pause)) {
            scheduler.getPlayer().setPaused(!scheduler.getPlayer().isPaused());
        } else if (buttonId.equals(next)) {
            event.getChannel().sendMessage(String.format("%sskip", System.getProperty("prefix"))).queue();
        }
    }
}
