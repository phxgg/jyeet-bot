package bot.music;

import bot.records.MessageType;
import dev.arbjerg.lavalink.protocol.v4.Track;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;

public class TrackBoxBuilder {
    private static final String PROGRESS_FILL = "\u25a0";
    private static final String PROGRESS_EMPTY = "\u2015";

    public static MessageEmbed buildTrackBox(int width, Track track, boolean isPaused, int volume, int queueSize) {
        return boxify(width, track, isPaused, volume, queueSize);
    }

    private static String buildDurationLine(int width, Track track, boolean isPaused) {
        String cornerText = isPaused ? "\u23F8" : "\uD83D\uDCFB";

        String duration = formatTiming(track.getInfo().getLength(), track.getInfo().getLength());
        String position = formatTiming(track.getInfo().getPosition(), track.getInfo().getLength());
        int spacing = duration.length() - position.length();
        int barLength = width - duration.length() - position.length() - spacing - 14;

        float progress = (float) Math.min(track.getInfo().getPosition(), track.getInfo().getLength()) / (float) Math.max(track.getInfo().getLength(), 1);
        int progressBlocks = Math.round(progress * barLength);

        StringBuilder builder = new StringBuilder();
        builder.append("```");
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
        builder.append("```");

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

    private static MessageEmbed boxify(int width, Track track, boolean isPaused, int volume, int queueSize) {
        EmbedBuilder eb = new EmbedBuilder();

        String duration = formatTiming(track.getInfo().getLength(), track.getInfo().getLength());

        eb.setAuthor("Now playing");
        eb.setTitle(String.format("%s - %s", track.getInfo().getAuthor(), track.getInfo().getTitle()), track.getInfo().getUri());
        eb.setColor(MessageType.TrackBox.color);
        eb.setDescription(buildDurationLine(width - 4, track, isPaused));
        eb.setThumbnail(track.getInfo().getArtworkUrl());

        eb.addField("Link", String.format("[Click](%s)", track.getInfo().getUri()), true);
        eb.addField("Duration", duration, true);
        eb.addField("In Queue", String.format("%d tracks", queueSize), true);
        eb.addField("Volume", volume + "%", true);
//        eb.addField("Requested By", String.format("<@%s>", ((TrackMetadata) track.getUserData()).getRequestedBy().getId()), true);

        eb.setFooter("YEEET", "https://yeeet-bot.netlify.app/assets/images/logo.png");

        return eb.build();
    }

    public static List<Button> sendButtons(String guildId) {
        List<Button> buttons = new ArrayList<>();

        // Button IDs should have a 'guild' prefix, so we can know in which guild the button was clicked.
        buttons.add(Button.secondary(String.format("%s_trackbox_previous", guildId), Emoji.fromCustom("previous", Long.parseLong("965604705298432020"), false)).asDisabled());
        buttons.add(Button.secondary(String.format("%s_trackbox_pause", guildId), Emoji.fromCustom("pause_play", Long.parseLong("965604705252282410"), false)));
        buttons.add(Button.secondary(String.format("%s_trackbox_next", guildId), Emoji.fromCustom("next", Long.parseLong("965604705139032074"), false)));
        buttons.add(Button.secondary(String.format("%s_trackbox_shuffle", guildId), Emoji.fromCustom("shuffle", Long.parseLong("965607534318719056"), false)));
        buttons.add(Button.secondary(String.format("%s_trackbox_stop", guildId), Emoji.fromCustom("stop", Long.parseLong("965604527619334154"), false)));

        return buttons;
    }
}
