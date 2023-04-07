package bot.records;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class InteractionResponse {
    private boolean success;
    private boolean ephemeral;
    private MessageType messageType;
    private String message;
    private boolean newMessage;

    public InteractionResponse(boolean success, boolean ephemeral, MessageType type, String message, boolean newMessage) {
        this.success = success;
        this.ephemeral = ephemeral;
        this.messageType = type;
        this.message = message;
        this.newMessage = newMessage;
    }

    public InteractionResponse() {
        this.success = true;
        this.ephemeral = false;
        this.messageType = MessageType.Info;
        this.message = "Interaction Response";
        this.newMessage = false;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public String getMessage() {
        return message;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public boolean isNewMessage() {
        return newMessage;
    }

    public InteractionResponse setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public InteractionResponse setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
        return this;
    }

    public InteractionResponse setMessageType(MessageType messageType) {
        this.messageType = messageType;
        return this;
    }

    public InteractionResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public InteractionResponse setNewMessage(boolean newMessage) {
        this.newMessage = newMessage;
        return this;
    }

    public static void handle(InteractionHook hook, InteractionResponse response) {
        if (hook == null || response == null)
            return;

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(response.getMessageType().color)
                .setDescription(response.getMessage());

        hook.setEphemeral(response.isEphemeral());

        if (response.isNewMessage()) {
            hook
                    .sendMessageEmbeds(embed.build())
                    .queue();
            return;
        }

        hook
                .editOriginalEmbeds(embed.build())
                .queue();
    }
}
