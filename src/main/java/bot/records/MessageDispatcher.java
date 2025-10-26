package bot.records;

import bot.controller.impl.music.TrackBoxBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class MessageDispatcher {
    private static final Logger log = LoggerFactory.getLogger(MessageDispatcher.class);
    private static final int deleteSeconds = 5;
    private final AtomicReference<TextChannel> outputChannel;

    public MessageDispatcher() {
        this.outputChannel = new AtomicReference<>();
    }

    public TextChannel getOutputChannel() {
        return this.outputChannel.get();
    }

    public void setOutputChannel(TextChannel channel) {
        this.outputChannel.set(channel);
    }

    public void sendTrackBoxMessage(
            MessageEmbed messageEmbed,
            Consumer<Message> success,
            Consumer<Throwable> failure) {
        if (outputChannel.get() == null) {
            return;
        }

        TextChannel channel = outputChannel.get();

        if (channel != null) {
//            channel.sendMessageEmbeds(messageEmbed).setActionRow(TrackBoxBuilder.sendButtons(channel.getGuild().getId()))
//                    .queue(success, failure);
            channel.sendMessageEmbeds(messageEmbed)
                    .addComponents(
                            ActionRow.of(
                                    TrackBoxBuilder.sendButtons(channel.getGuild().getId())
                            )
                    )
                    .queue(success, failure);
        }
    }

    public void sendDisposableMessage(MessageType type, String message) {
        if (outputChannel.get() == null) {
            return;
        }

        TextChannel channel = outputChannel.get();

        EmbedBuilder eb = new EmbedBuilder();
//            eb.setTitle(type.toString());
        eb.setColor(type.color);
        eb.setDescription(message);

        channel.sendMessageEmbeds(eb.build()).queue(m -> m.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS));
    }

    public static EmbedBuilder createEmbedMessage(MessageType type, String message) {
        return new EmbedBuilder().setColor(type.color).setDescription(message);
    }
}
