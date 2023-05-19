package bot.api.entities;

public class Server {
    private String id;
    private String guildId;
    private String name;
    private String ownerId;
    private String prefix;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return this.id;
    }

    public String getGuildId() {
        return this.guildId;
    }

    public String getName() {
        return this.name;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }
}
