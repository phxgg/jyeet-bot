package bot.listeners;

import bot.controller.IBotController;
import bot.controller.impl.music.MusicController;
import bot.controller.impl.music.MusicScheduler;
import bot.records.ActionData;
import bot.records.InteractionResponse;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ButtonComponentClick extends ListenerAdapter {
    private final BotApplicationManager applicationManager;

    public ButtonComponentClick(BotApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    public BotApplicationManager getApplicationManager() {
        return applicationManager;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isAcknowledged())
            event.deferEdit().queue();

        ActionData ad = null;

        try {
            ad = new ActionData(
                    event,
                    event.getHook(),
                    Objects.requireNonNull(event.getGuild()).getAudioManager()
            );
        } catch (NullPointerException e) {
            System.out.println("[ButtonComponentClick] audioManager is null.");
            e.printStackTrace();
            return;
        }

        if (!MusicController.canPerformAction(ad, true))
            return;

        final String previous = event.getGuild().getId() + "_trackbox_previous";
        final String pause = event.getGuild().getId() + "_trackbox_pause";
        final String next = event.getGuild().getId() + "_trackbox_next";
        final String shuffle = event.getGuild().getId() + "_trackbox_shuffle";
        final String stop = event.getGuild().getId() + "_trackbox_stop";

        String buttonId = event.getButton().getId();

        // Get music scheduler for Guild
        MusicScheduler scheduler = null;
        for (Class<? extends IBotController> controllerClass : applicationManager.getContext(event.getGuild()).getControllers().keySet()) {
            IBotController controller = applicationManager.getContext(event.getGuild()).getControllers().get(controllerClass);
            if (controller instanceof MusicController) {
                scheduler = ((MusicController) controller).getScheduler();
                break;
            }
        }

        if (scheduler == null || buttonId == null) {
            System.out.println("[ButtonComponentClick] scheduler or buttonId is null.");
            return;
        }

        if (buttonId.equals(previous)) {
            scheduler.playPrevious();
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
