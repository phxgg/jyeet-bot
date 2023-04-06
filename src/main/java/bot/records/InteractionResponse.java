package bot.records;

public class InteractionResponse {
    private boolean success;
    private MessageType messageType;
    private String message;
    private boolean newMessage;

    public InteractionResponse(boolean success, MessageType type, String message, boolean newMessage) {
        this.success = success;
        this.messageType = type;
        this.message = message;
        this.newMessage = newMessage;
    }

    public InteractionResponse() {
        this.success = true;
        this.messageType = MessageType.Info;
        this.message = "Interaction Response";
        this.newMessage = false;
    }

    public boolean isSuccess() {
        return success;
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
}
