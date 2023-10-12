package bot.records;

import java.awt.*;

public enum MessageType {
    Info(Color.DARK_GRAY),
    Success(new Color(0x2ECC71)), // dark green
    Error(new Color(0xE74C3C)), // dark red
    Warning(Color.ORANGE),
    TrackBox(Color.PINK);

    public final Color color;

    MessageType(Color color) {
        this.color = color;
    }
}
