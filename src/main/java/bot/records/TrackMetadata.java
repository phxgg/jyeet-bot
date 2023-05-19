package bot.records;

import net.dv8tion.jda.api.entities.User;

public class TrackMetadata {
    private User requestedBy;

    public TrackMetadata() {
    }

    public User getRequestedBy() {
        return this.requestedBy;
    }

    public TrackMetadata setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
        return this;
    }
}
