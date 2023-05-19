package bot.records;

import bot.controller.IBotController;

import java.util.HashMap;
import java.util.Map;

public class BotGuildContext {
    private final long guildId;
    private String guildPrefix;
    private final Map<Class<? extends IBotController>, IBotController> controllers;

    public BotGuildContext(long guildId, String guildPrefix) {
        this.guildId = guildId;
        this.guildPrefix = guildPrefix;
        this.controllers = new HashMap<>();
    }

    public long getGuildId() {
        return this.guildId;
    }

    public String getGuildPrefix() {
        return this.guildPrefix;
    }

    public Map<Class<? extends IBotController>, IBotController> getControllers() {
        return this.controllers;
    }

    public void setGuildPrefix(String guildPrefix) {
        this.guildPrefix = guildPrefix;
    }

    public void addController(Class<? extends IBotController> controllerClass, IBotController controller) {
        this.controllers.put(controllerClass, controller);
    }

    public void removeController(Class<? extends IBotController> controllerClass) {
        this.controllers.remove(controllerClass);
    }
}
