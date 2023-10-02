package bot.initialization;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class SetupCommands {
    public static void Setup(JDA jda) {
        jda.updateCommands().addCommands(
//                    Commands.slash("playlocal", "play local track"),
                Commands.slash("help", "Shows the help embed box.")
                        .setGuildOnly(true),
                Commands.slash("play", "Starts playing something.")
                        .setGuildOnly(true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "query", "Could be a song, playlist or album name or link. YouTube & Spotify.")
                                        .setRequired(true)),
                Commands.slash("stop", "Stop player.")
                        .setGuildOnly(true),
                Commands.slash("pause", "Pause current playing track. Use again to unpause.")
                        .setGuildOnly(true),
                Commands.slash("resume", "Resume player.")
                        .setGuildOnly(true),
                Commands.slash("shuffle", "Shuffles the queue.")
                        .setGuildOnly(true),
                Commands.slash("skip", "Skips the current song.")
                        .setGuildOnly(true),
                Commands.slash("disconnect", "Disconnect player.")
                        .setGuildOnly(true),
                Commands.slash("playnow", "Destroys current queue and plays whatever provided.")
                        .setGuildOnly(true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "query", "Could be a song, playlist or album name or link. YouTube & Spotify.")
                                        .setRequired(true)),
                Commands.slash("playnext", "Adds in queue whatever provided right after the current track.")
                        .setGuildOnly(true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "query", "Could be a song, playlist or album name or link. YouTube & Spotify.")
                                        .setRequired(true)),
                Commands.slash("previous", "Plays previous track.")
                        .setGuildOnly(true),
                Commands.slash("history", "Shows track history.")
                        .setGuildOnly(true),
                Commands.slash("song", "Shows current playing track.")
                        .setGuildOnly(true),
                Commands.slash("queue", "Display current queue list.")
                        .setGuildOnly(true),
                Commands.slash("clearqueue", "Clears the queue.")
                        .setGuildOnly(true),
                Commands.slash("volume", "Set the volume player.")
                        .setGuildOnly(true)
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "value", "0-100")
                                        .setRequired(true)
                                        .setRequiredRange(0, 100)),
                Commands.slash("forward", "Forward track by given seconds.")
                        .setGuildOnly(true)
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "seconds", "Number of seconds you want to forward.")
                                        .setRequired(true)),
                Commands.slash("backward", "Backward track by given seconds.")
                        .setGuildOnly(true)
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "seconds", "Number of seconds you want to forward.")
                                        .setRequired(true)),
                Commands.slash("setoutputchannel", "[ADMIN] Set output channel to current message channel.")
                        .setGuildOnly(true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                ,Commands.slash("util", "Utility commands.")
                        .setGuildOnly(true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .addSubcommands(
                                new SubcommandData("context", "Show current guild context.")
                        )
//                ,Commands.slash("bank", "Banking commands.")
//                        .setGuildOnly(true)
//                        .addSubcommands(
//                                new SubcommandData("balance", "Shows your balance."),
//                                new SubcommandData("deposit", "Deposit money to your bank.")
//                                        .addOptions(
//                                                new OptionData(OptionType.INTEGER, "amount", "Amount of money you want to deposit.")
//                                                        .setRequired(true)),
//                                new SubcommandData("withdraw", "Withdraw money from your bank.")
//                                        .addOptions(
//                                                new OptionData(OptionType.INTEGER, "amount", "Amount of money you want to withdraw.")
//                                                        .setRequired(true))
//                        )
        ).queue();

        System.out.println("Commands updated.");
        System.out.println("Logged in as " + jda.getSelfUser().getName());
    }
}
