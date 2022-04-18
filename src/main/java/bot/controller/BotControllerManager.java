package bot.controller;

import bot.BotApplicationManager;
import bot.BotGuildContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bot.music.MusicController;
import bot.records.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

public class BotControllerManager {
    private final List<BotControllerFactory> controllerFactories;
    private final Map<String, Command> commands;

    public BotControllerManager() {
        controllerFactories = new ArrayList<>();
        commands = new HashMap<>();
    }

    public void registerController(BotControllerFactory factory) {
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
        String usage = annotation.usage().isEmpty() ? null : annotation.usage();

        Parameter[] methodParameters = method.getParameters();
        if (methodParameters.length == 0 || !methodParameters[0].getType().isAssignableFrom(Message.class)) {
            return;
        }

        method.setAccessible(true);

        List<Class<?>> parameters = new ArrayList<>();
        for (int i = 1; i < methodParameters.length; i++) {
            parameters.add(methodParameters[i].getType());
        }

        Command command = new Command(commandName, usage, parameters, controllerClass, method);
        commands.put(command.getName(), command);
    }

    public void destroyPlayer(Map<Class<? extends BotController>, BotController> instances) {
        instances.forEach((controllerClass, controller) -> {
            if (controller instanceof MusicController) {
                ((MusicController) controller).destroyPlayer();
                MusicController.removeTrackBoxButtonClickListener(((MusicController) controller).getGuild());
            }
        });
    }

    public void dispatchMessage(
            Map<Class<? extends BotController>, BotController> instances,
            String prefix,
            Message message,
            BotCommandMappingHandler handler
    ) {

        String content = message.getContentDisplay().trim();
        String[] separated = content.split("\\s+", 2);

        if (!separated[0].startsWith(prefix)) {
            return;
        }

        String commandName = separated[0].substring(prefix.length());
        Command command = commands.get(commandName);

        if (command == null) {
            handler.commandNotFound(message, commandName);
            return;
        }

        String[] inputArguments = separated.length == 1 ? new String[0] : separated[1].split("\\s+", command.getParameters().size());

        if (inputArguments.length != command.getParameters().size()) {
            handler.commandWrongParameterCount(message, command.getName(), command.getUsage(), inputArguments.length, command.getParameters().size());
            return;
        }

        Object[] arguments = new Object[command.getParameters().size() + 1];
        arguments[0] = message;

        for (int i = 0; i < command.getParameters().size(); i++) {
            Class<?> parameterClass = command.getParameters().get(i);

            try {
                arguments[i + 1] = parseArgument(parameterClass, inputArguments[i]);
            } catch (IllegalArgumentException ignored) {
                handler.commandWrongParameterType(message, command.getName(), command.getUsage(), i, inputArguments[i], parameterClass);
                return;
            }
        }

        try {
            command.getCommandMethod().invoke(instances.get(command.getControllerClass()), arguments);
        } catch (InvocationTargetException e) {
            handler.commandException(message, command.getName(), e.getCause());
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

    public List<BotController> createControllers(BotApplicationManager applicationManager, BotGuildContext context, Guild guild) {
        List<BotController> controllers = new ArrayList<>();
        for (BotControllerFactory factory : controllerFactories) {
            controllers.add(factory.create(applicationManager, context, guild));
        }
        return controllers;
    }
}
