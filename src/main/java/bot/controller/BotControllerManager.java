package bot.controller;

import bot.listeners.BotApplicationManager;
import bot.records.BotGuildContext;
import bot.music.MusicController;
import bot.records.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BotControllerManager {
    @SuppressWarnings("rawtypes")
    private final List<IBotControllerFactory> controllerFactories;
    private final Map<String, Command> commands;

    public BotControllerManager() {
        controllerFactories = new ArrayList<>();
        commands = new HashMap<>();
    }

    @SuppressWarnings("rawtypes")
    public void registerController(IBotControllerFactory factory) {
        controllerFactories.add(factory);

        Class<?> controllerClass = factory.getControllerClass();

        for (Method method : controllerClass.getDeclaredMethods()) {
            BotCommandHandler annotation = method.getAnnotation(BotCommandHandler.class);

            if (annotation != null) {
                registerControllerMethod(controllerClass, method, annotation);
            }
        }
    }

    private void registerControllerMethod(Class<?> controllerClass, Method method, BotCommandHandler annotation) {
        String commandName = annotation.name().isEmpty() ? method.getName().toLowerCase() : annotation.name();
        String description = annotation.description().isEmpty() ? "No description provided." : annotation.description();
        String usage = annotation.usage().isEmpty() ? null : annotation.usage();

        Parameter[] methodParameters = method.getParameters();
        if (methodParameters.length == 0 || !methodParameters[0].getType().isAssignableFrom(SlashCommandInteractionEvent.class)) {
            return;
        }

        method.setAccessible(true);

        List<Class<?>> parameters = new ArrayList<>();
        for (int i = 1; i < methodParameters.length; i++) {
            parameters.add(methodParameters[i].getType());
        }

        Command command = new Command(commandName, description, usage, parameters, controllerClass, method);
        commands.put(command.getName(), command);
    }

    public void destroyPlayer(Map<Class<? extends IBotController>, IBotController> instances) {
        instances.forEach((controllerClass, controller) -> {
            if (controller instanceof MusicController) {
                ((MusicController) controller).destroyPlayer();
                MusicController.removeTrackBoxButtonClickListener(((MusicController) controller).getGuild());
            }
        });
    }

    public void waitInVC(Map<Class<? extends IBotController>, IBotController> instances) {
        instances.forEach((controllerClass, controller) -> {
            if (controller instanceof MusicController) {
                ((MusicController) controller).getScheduler().waitInVC();
            }
        });
    }

    public void dispatchSlashCommand(
            Map<Class<? extends IBotController>, IBotController> instances,
            SlashCommandInteractionEvent event,
            IBotSlashCommandMappingHandler handler
    ) {
        String commandName = event.getName();

        Command command = commands.get(commandName);

        if (command == null) {
            handler.commandNotFound(commandName);
            return;
        }

        Object[] arguments = new Object[command.getParameters().size() + 1];
        arguments[0] = event;

        for (int i = 0; i < event.getOptions().size(); i++) {
            Class<?> parameterClass = command.getParameters().get(i);

            try {
                arguments[i + 1] = parseArgument(parameterClass, event.getOptions().get(i).getAsString());
            } catch (IllegalArgumentException ignored) {
                handler.commandWrongParameterType(
                        command.getName(),
                        command.getDescription(),
                        command.getUsage(),
                        i,
                        event.getOptions().get(i).getAsString(),
                        parameterClass
                );
                return;
            }
        }

        try {
            command.getCommandMethod().invoke(instances.get(command.getControllerClass()), arguments);
        } catch (InvocationTargetException e) {
            handler.commandException(command.getName(), e.getCause());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseArgument(Class<?> parameterClass, String value) {
        try {
            if (parameterClass == String.class) {
                return value;
            } else if (parameterClass == int.class || parameterClass == Integer.class) {
                return Integer.valueOf(value);
            } else if (parameterClass == long.class || parameterClass == Long.class) {
                return Long.valueOf(value);
            } else if (parameterClass == boolean.class || parameterClass == Boolean.class) {
                return parseBooleanArgument(value);
            } else if (parameterClass == float.class || parameterClass == Float.class) {
                return Float.valueOf(value);
            } else if (parameterClass == double.class || parameterClass == Double.class) {
                return Double.valueOf(value);
            } else {
                throw new IllegalArgumentException();
            }
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException();
        }
    }

    private boolean parseBooleanArgument(String value) {
        if ("yes".equals(value) || "true".equals(value)) {
            return true;
        } else if ("no".equals(value) || "false".equals(value)) {
            return false;
        } else {
            int integerValue = Integer.parseInt(value);

            if (integerValue == 1) {
                return true;
            } else if (integerValue == 0) {
                return false;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public List<IBotController> createControllers(BotApplicationManager applicationManager, BotGuildContext context, Guild guild) {
        List<IBotController> controllers = new ArrayList<>();
        for (IBotControllerFactory factory : controllerFactories) {
            controllers.add(factory.create(applicationManager, context, guild));
        }
        return controllers;
    }
}
