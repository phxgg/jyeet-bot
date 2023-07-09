package bot.utility;

import bot.controller.BotCommandHandler;
import bot.controller.IBotController;
import bot.controller.IBotControllerFactory;
import bot.listeners.BotApplicationManager;
import bot.records.BotGuildContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class UtilityController implements IBotController {
    private final BotApplicationManager manager;
    private final BotGuildContext state;
    private final Guild guild;

    public UtilityController(BotApplicationManager manager, BotGuildContext state, Guild guild) {
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

    @BotCommandHandler(name = "util context", description = "Get current guild context", usage = "context")
    public void commandContext(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Guild Context");
        embed.addField("Guild ID", this.guild.getId(), true);
        embed.addField("Guild Name", this.guild.getName(), true);
        embed.addField("", "", true);

        for (Class<? extends IBotController> controllerClass : this.state.getControllers().keySet()) {
            embed.addField("Controller", controllerClass.getSimpleName(), false);
        }

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    public static class Factory implements IBotControllerFactory<UtilityController> {
        @Override
        public Class<UtilityController> getControllerClass() {
            return UtilityController.class;
        }

        @Override
        public UtilityController create(BotApplicationManager manager, BotGuildContext state, Guild guild) {
            return new UtilityController(manager, state, guild);
        }
    }
}
