package bot.dto;

public class User {
    private String id;
    private String username;
    private String email;
    private String createdAt;
    private String updatedAt;
    private String[] roles;

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String[] getRoles() {
        return roles;
    }
}
