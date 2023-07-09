package bot.bank;

import bot.controller.BotCommandHandler;
import bot.controller.IBotController;
import bot.controller.IBotControllerFactory;
import bot.listeners.BotApplicationManager;
import bot.records.BotGuildContext;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class BankController implements IBotController {
    private final BotApplicationManager manager;
    private final BotGuildContext state;
    private final Guild guild;

    public BankController(BotApplicationManager manager, BotGuildContext state, Guild guild) {
        this.manager = manager;
        this.state = state;
        this.guild = guild;
    }

    public BotApplicationManager getManager() {
        return this.manager;
    }

    public BotGuildContext getState() {
        return this.state;
    }

    public Guild getGuild() {
        return this.guild;
    }

    @BotCommandHandler(name = "bank deposit", description = "Deposit money to bank.", usage = "deposit <amount>")
    public void commandDeposit(SlashCommandInteractionEvent event, int amount) {
        return;
    }

    @BotCommandHandler(name = "bank withdraw", description = "Withdraw money from bank.", usage = "withdraw <amount>")
    public void commandWithdraw(SlashCommandInteractionEvent event, int amount) {
        return;
    }

    @BotCommandHandler(name = "bank balance", description = "Check your bank balance.", usage = "balance")
    public void commandBalance(SlashCommandInteractionEvent event) {
        return;
    }

    public static class Factory implements IBotControllerFactory<BankController> {
        @Override
        public Class<BankController> getControllerClass() {
            return BankController.class;
        }

        @Override
        public BankController create(BotApplicationManager manager, BotGuildContext state, Guild guild) {
            return new BankController(manager, state, guild);
        }
    }
}
