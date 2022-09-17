package bot;

import java.awt.*;

public enum MessageType {
    Info(Color.CYAN),
    Success(Color.GREEN),
    Error(Color.RED),
    Warning(Color.ORANGE),
    TrackBox(Color.PINK);

    public final Color color;

    MessageType(Color color) {
        this.color = color;
    }
}
