package bot.records;

import java.util.function.Consumer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public interface MessageDispatcher {
    Integer deleteSeconds = 5;

    void sendMessage(MessageType type, MessageEmbed message, Consumer<Message> success, Consumer<Throwable> failure, final boolean isTrackBox);

    void sendMessage(MessageType type, String message);

    void sendDisposableMessage(MessageType type, String message);

    void reply(MessageChannel channel, MessageType type, String message);

    void replyDisposable(MessageChannel channel, MessageType type, String message);

    EmbedBuilder createEmbedMessage(MessageType type, String message);
}
