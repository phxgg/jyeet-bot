package bot.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TrackBoxBuilder {
    private static final String PROGRESS_FILL = "\u25a0";
    private static final String PROGRESS_EMPTY = "\u2015";

    public static MessageEmbed buildTrackBox(int width, AudioTrack track, boolean isPaused, int volume, int queueSize) {
        return boxify(width, track, isPaused, volume, queueSize);
    }

    private static String buildDurationLine(int width, AudioTrack track, boolean isPaused) {
        String cornerText = isPaused ? "\u23F8" : "\uD83D\uDCFB";

        String duration = formatTiming(track.getDuration(), track.getDuration());
        String position = formatTiming(track.getPosition(), track.getDuration());
        int spacing = duration.length() - position.length();
        int barLength = width - duration.length() - position.length() - spacing - 14;

        float progress = (float) Math.min(track.getPosition(), track.getDuration()) / (float) Math.max(track.getDuration(), 1);
        int progressBlocks = Math.round(progress * barLength);

        StringBuilder builder = new StringBuilder();
        builder.append("`");

        builder.append(" ".repeat(3 - cornerText.length()));

        builder.append(cornerText);

        builder.append(" [");
        for (int i = 0; i < barLength; i++) {
            builder.append(i < progressBlocks ? PROGRESS_FILL : PROGRESS_EMPTY);
        }
        builder.append("]");

        builder.append(" ".repeat(Math.max(0, spacing + 1)));

        builder.append(position);
        builder.append(" of ");
        builder.append(duration);

        builder.append("`");

        return builder.toString();
    }

    private static String formatTiming(long timing, long maximum) {
        timing = Math.min(timing, maximum) / 1000;

        long seconds = timing % 60;
        timing /= 60;
        long minutes = timing % 60;
        timing /= 60;
        long hours = timing;

        if (maximum >= 3600000L) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private static MessageEmbed boxify(int width, AudioTrack track, boolean isPaused, int volume, int queueSize) {
        EmbedBuilder eb = new EmbedBuilder();

        String duration = formatTiming(track.getDuration(), track.getDuration());

        eb.setColor(Color.PINK);
        eb.setTitle(String.format(":dvd: Now playing: %s", track.getInfo().title));
        eb.setDescription(buildDurationLine(width - 4, track, isPaused));

//        eb.setThumbnail(track.getInfo().uri);
        eb.addField("Duration", duration, true);
        eb.addField("Link", String.format("[Click here](%s)", track.getInfo().uri), true);
        eb.addField("Volume", volume + "%", true);
//        eb.addField("Requested by", String.format("%s"), true);

        eb.setFooter("Tracks in queue: " + queueSize);

        return eb.build();
    }

    public static List<Button> sendButtons(String guildId) {
        List<Button> buttons = new ArrayList<>();

        // Button IDs should have a 'guild' prefix, so we can know in which guild the button was clicked.
        buttons.add(Button.secondary(String.format("%s_trackbox_previous", guildId), Emoji.fromUnicode("U+23EE")).asDisabled());
        buttons.add(Button.primary(String.format("%s_trackbox_pause", guildId), Emoji.fromUnicode("U+23EF")));
        buttons.add(Button.secondary(String.format("%s_trackbox_next", guildId), Emoji.fromUnicode("U+23ED")));
        buttons.add(Button.success(String.format("%s_trackbox_shuffle", guildId), Emoji.fromUnicode("U+1F500")));
        buttons.add(Button.danger(String.format("%s_trackbox_stop", guildId), Emoji.fromUnicode("U+23F9")));

        return buttons;
    }
}
