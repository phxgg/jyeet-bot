package bot.controller;

public interface BotSlashCommandMappingHandler {
    void commandNotFound(String name);

    void commandWrongParameterCount(String name, String description, String usage, int given, int required);

    void commandWrongParameterType(String name, String description, String usage, int index, String value, Class<?> expectedType);

    void commandRestricted(String name);

    void commandException(String name, Throwable throwable);
}
