package bot;

import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public interface MessageDispatcher {
    public static Integer deleteSeconds = 5;

    void sendMessage(MessageEmbed message, Consumer<Message> success, Consumer<Throwable> failure);

    void sendMessage(String message);

    void sendDisposableMessage(String message);
}
