package bot.initialization;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionContextType;
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
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("play", "Starts playing something.")
                        .setContexts(InteractionContextType.GUILD)
                        .addOptions(
                                new OptionData(OptionType.STRING, "query", "Could be a song, playlist or album name or link. YouTube & Spotify.")
                                        .setRequired(true)),
                Commands.slash("stop", "Stop player.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("pause", "Pause current playing track. Use again to unpause.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("resume", "Resume player.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("shuffle", "Shuffles the queue.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("skip", "Skips the current song.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("disconnect", "Disconnect player.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("playnow", "Destroys current queue and plays whatever provided.")
                        .setContexts(InteractionContextType.GUILD)
                        .addOptions(
                                new OptionData(OptionType.STRING, "query", "Could be a song, playlist or album name or link. YouTube & Spotify.")
                                        .setRequired(true)),
                Commands.slash("playnext", "Adds in queue whatever provided right after the current track.")
                        .setContexts(InteractionContextType.GUILD)
                        .addOptions(
                                new OptionData(OptionType.STRING, "query", "Could be a song, playlist or album name or link. YouTube & Spotify.")
                                        .setRequired(true)),
                Commands.slash("previous", "Plays previous track.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("history", "Shows track history.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("song", "Shows current playing track.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("queue", "Display current queue list.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("clearqueue", "Clears the queue.")
                        .setContexts(InteractionContextType.GUILD),
                Commands.slash("volume", "Set the volume player.")
                        .setContexts(InteractionContextType.GUILD)
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "value", "0-100")
                                        .setRequired(true)
                                        .setRequiredRange(0, 100)),
                Commands.slash("forward", "Forward track by given seconds.")
                        .setContexts(InteractionContextType.GUILD)
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "seconds", "Number of seconds you want to forward.")
                                        .setRequired(true)),
                Commands.slash("backward", "Backward track by given seconds.")
                        .setContexts(InteractionContextType.GUILD)
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "seconds", "Number of seconds you want to forward.")
                                        .setRequired(true)),
                Commands.slash("setoutputchannel", "[ADMIN] Set output channel to current message channel.")
                        .setContexts(InteractionContextType.GUILD)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                ,Commands.slash("util", "Utility commands.")
                        .setContexts(InteractionContextType.GUILD)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .addSubcommands(
                                new SubcommandData("context", "Show current guild context.")
                        )
//                ,Commands.slash("bank", "Banking commands.")
//                        .setContexts(InteractionContextType.GUILD)
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
