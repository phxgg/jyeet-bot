package bot;

import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public interface MessageDispatcher {
    Integer deleteSeconds = 5;

    void sendMessage(MessageType type, MessageEmbed message, Consumer<Message> success, Consumer<Throwable> failure);

    void sendMessage(MessageType type, String message);

    void sendDisposableMessage(MessageType type, String message);
}
