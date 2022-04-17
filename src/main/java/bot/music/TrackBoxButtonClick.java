package bot.music;

import bot.MessageDispatcher;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
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

        Message message = event.getMessage();
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();

        final String previous = scheduler.getGuild().getId() + "_trackbox_previous";
        final String pause = scheduler.getGuild().getId() + "_trackbox_pause";
        final String next = scheduler.getGuild().getId() + "_trackbox_next";

        String buttonId = event.getButton().getId();

        assert buttonId != null;
        if (buttonId.equals(previous)) {
            if (!MusicController.canPerformAction(scheduler.getMessageDispatcher(), message, audioManager))
                return;
            event.getChannel().sendMessage("Back button clicked").queue();
        } else if (buttonId.equals(pause)) {
            if (!MusicController.canPerformAction(scheduler.getMessageDispatcher(), message, audioManager))
                return;
            scheduler.getPlayer().setPaused(!scheduler.getPlayer().isPaused());
        } else if (buttonId.equals(next)) {
            if (!MusicController.canPerformAction(scheduler.getMessageDispatcher(), message, audioManager))
                return;
            event.getChannel().sendMessage(String.format("%sskip", System.getProperty("prefix"))).queue();
        }
    }
}
