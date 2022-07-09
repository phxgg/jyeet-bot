package bot;

import bot.controller.BotController;

import java.util.HashMap;
import java.util.Map;

public class BotGuildContext {
    public final long guildId;
    public String guildPrefix;
    public final Map<Class<? extends BotController>, BotController> controllers;

    public BotGuildContext(long guildId, String guildPrefix) {
        this.guildId = guildId;
        this.guildPrefix = guildPrefix;
        this.controllers = new HashMap<>();
    }
}
