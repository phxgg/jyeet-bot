package bot.controller;

import bot.listeners.BotApplicationManager;
import bot.records.BotGuildContext;
import net.dv8tion.jda.api.entities.Guild;

public interface IBotControllerFactory<T extends IBotController> {
    Class<T> getControllerClass();

    T create(BotApplicationManager manager, BotGuildContext state, Guild guild);
}
