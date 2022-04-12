package bot.controller;

import bot.BotApplicationManager;
import bot.BotGuildContext;
import net.dv8tion.jda.api.entities.Guild;

public interface BotControllerFactory<T extends bot.controller.BotController> {
    Class<T> getControllerClass();

    T create(BotApplicationManager manager, BotGuildContext state, Guild guild);
}
