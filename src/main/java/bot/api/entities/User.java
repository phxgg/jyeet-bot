package bot.api.entities;

public class User {
    private String id;
    private String username;
    private String email;
    private String createdAt;
    private String updatedAt;
    private String[] roles;

    public String getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public String[] getRoles() {
        return this.roles;
    }
}
