package bot.dto;

public class Server {
    private String id;
    private int guildId;
    private String name;
    private String ownerId;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public int getGuildId() {
        return guildId;
    }

    public String getName() {
        return name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
