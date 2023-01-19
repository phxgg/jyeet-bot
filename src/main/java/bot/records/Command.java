package bot.records;

import java.lang.reflect.Method;
import java.util.List;

public class Command {
    private final String name;
    private final String description;
    private final String usage;
    private final List<Class<?>> parameters;
    private final Class<?> controllerClass;
    private final Method commandMethod;

    public Command(
            String name,
            String description,
            String usage,
            List<Class<?>> parameters,
            Class<?> controllerClass,
            Method commandMethod
    ) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.parameters = parameters;
        this.controllerClass = controllerClass;
        this.commandMethod = commandMethod;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public List<Class<?>> getParameters() {
        return parameters;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getCommandMethod() {
        return commandMethod;
    }
}
