package bot.listeners;

import bot.music.MusicController;
import bot.music.MusicScheduler;
import bot.records.ActionData;
import bot.records.InteractionResponse;
import bot.records.MessageType;
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
                    event.getHook(),
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
            InteractionResponse response = new InteractionResponse()
                    .setSuccess(false)
                    .setEphemeral(true)
                    .setNewMessage(true)
                    .setMessageType(MessageType.Warning)
                    .setMessage("<@" + event.getInteraction().getUser().getId() + ">" + "Under construction");
            InteractionResponse.handle(event.getHook(), response);
        } else if (buttonId.equals(pause)) {
            scheduler.getPlayer().setPaused(!scheduler.getPlayer().isPaused());
        } else if (buttonId.equals(next)) {
            scheduler.skip();
        } else if (buttonId.equals(shuffle)) {
            InteractionResponse response = scheduler.shuffleQueue();
            response.setNewMessage(true);
            response.setMessage("<@" + event.getInteraction().getUser().getId() + "> " + response.getMessage());
            InteractionResponse.handle(event.getHook(), response);
        } else if (buttonId.equals(stop)) {
            InteractionResponse response = scheduler.stopPlayer();
            response.setNewMessage(true);
            response.setMessage("<@" + event.getInteraction().getUser().getId() + "> " + response.getMessage());
            InteractionResponse.handle(event.getHook(), response);
//            event.getGuild().getAudioManager().closeAudioConnection();
        }
    }
}
