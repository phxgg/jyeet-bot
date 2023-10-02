package bot.records;

import net.dv8tion.jda.api.entities.User;

public class TrackMetadata {
    private User requestedBy;
    private boolean addInHistory;

    public TrackMetadata() {
        this.requestedBy = null;
        this.addInHistory = true;
    }

    public User getRequestedBy() {
        return this.requestedBy;
    }

    public boolean shouldAddInHistory() {
        return this.addInHistory;
    }

    public TrackMetadata setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
        return this;
    }

    public TrackMetadata setAddInHistory(boolean addInHistory) {
        this.addInHistory = addInHistory;
        return this;
    }

    public TrackMetadata clone() {
        return new TrackMetadata().setRequestedBy(this.requestedBy).setAddInHistory(this.addInHistory);
    }
}
