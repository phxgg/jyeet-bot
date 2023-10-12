package bot.listeners;

import bot.records.BotGuildContext;
import bot.records.MessageType;
import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.controller.IBotController;
import bot.controller.BotControllerManager;
import bot.controller.IBotSlashCommandMappingHandler;
import bot.api.entities.Response;
import bot.api.entities.Server;
import bot.music.MusicController;
import bot.utility.UtilityController;
import com.google.gson.Gson;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import dev.arbjerg.lavalink.client.LavalinkClient;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BotApplicationManager extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(BotApplicationManager.class);

    private final LavalinkClient lavalinkClient;
    private final Map<Long, BotGuildContext> guildContexts;
    private final BotControllerManager controllerManager;
    private final ScheduledExecutorService executorService; // Interface
    private final Gson gson;

    public BotApplicationManager(LavalinkClient _lavalinkClient) {
        gson = new Gson();
        lavalinkClient = _lavalinkClient;
        guildContexts = new HashMap<>();
        controllerManager = new BotControllerManager();

        controllerManager.registerController(new MusicController.Factory());
        controllerManager.registerController(new UtilityController.Factory());
//        controllerManager.registerController(new BankController.Factory());

        executorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("bot"));
    }

    public ScheduledExecutorService getExecutorService() {
        return this.executorService;
    }

    public LavalinkClient getLavalinkClient() {
        return this.lavalinkClient;
    }

    public BotControllerManager getControllerManager() {
        return this.controllerManager;
    }

    private BotGuildContext createGuildState(long guildId, Guild guild) {
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", String.valueOf(guildId));
        }};

        String post = WebReq.Post("/servers/guild", data);
        Response r = gson.fromJson(post, Response.class);

        String prefix;

        if (r.getCode() == StatusCodes.OK.getCode()) {
            Server server = gson.fromJson(gson.toJson(r.getData()), Server.class);
            prefix = server.getPrefix();
        } else {
            prefix = System.getProperty("prefix");
        }

        BotGuildContext context = new BotGuildContext(guildId, prefix);

        for (IBotController controller : controllerManager.createControllers(this, context, guild)) {
            context.getControllers().put(controller.getClass(), controller);
        }

        return context;
    }

    public synchronized BotGuildContext getContext(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        BotGuildContext context = guildContexts.get(guildId);

        if (context == null) {
            context = createGuildState(guildId, guild);
            guildContexts.put(guildId, context);
        }

        return context;
    }

    public synchronized BotGuildContext getContextById(long guildId) {
        BotGuildContext context = guildContexts.get(guildId);

//        if (context == null) {
//            context = createGuildState(guildId, guild);
//            guildContexts.put(guildId, context);
//        }

        return context;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        Member member = event.getMember();

        if (event.getGuild() == null || member == null || member.getUser().isBot()) {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setTitle("Error");
            eb.setColor(MessageType.Error.color);
            eb.setDescription("You can't use commands in DMs.");

            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        BotGuildContext guildContext = getContext(event.getGuild());
//        String prefix = guildContext.getGuildPrefix();

        controllerManager.dispatchSlashCommand(guildContext.getControllers(), event, new IBotSlashCommandMappingHandler() {
            @Override
            public void commandNotFound(String name) {

            }

            @Override
            public void commandWrongParameterCount(String name, String description, String usage, int given, int required) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription("Wrong argument count.");
                eb.setFooter("Command: " + name);

//                event.getMessageChannel().sendMessageEmbeds(eb.build()).queue();
                event.getHook().setEphemeral(true).editOriginalEmbeds(eb.build()).queue();
            }

            @Override
            public void commandWrongParameterType(String name, String description, String usage, int index, String value, Class<?> expectedType) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription("Wrong argument type.");
                eb.setFooter("Command: " + name);

                event.getHook().setEphemeral(true).editOriginalEmbeds(eb.build()).queue();
            }

            @Override
            public void commandRestricted(String name) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription("Command not permitted.");
                eb.setFooter("Command: " + name);

                event.getHook().setEphemeral(true).editOriginalEmbeds(eb.build()).queue();
            }

            @Override
            public void commandException(String name, Throwable throwable) {
                EmbedBuilder eb = new EmbedBuilder();

                eb.setTitle("Error");
                eb.setColor(MessageType.Error.color);
                eb.setDescription(
                        String.format("Command threw an exception:\n`%s`\n```%s```",
                                throwable.getClass().getSimpleName(),
                                throwable.getMessage())
                );
                eb.setFooter("Command: " + name);

                event.getHook().setEphemeral(true).editOriginalEmbeds(eb.build()).queue();

//                log.error("Command with content {} threw an exception.", message.getContentDisplay(), throwable);
            }
        });

//        event.getHook().deleteOriginal().queue();
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        BotGuildContext context = getContext(event.getGuild());

        // Get number of members in voice channel
        // If there's only one member in the channel, check if it's the bot.
        // If it is, disconnect the voice channel.

        // Fix warnings
        if (event.getChannelLeft() == null)
            return;

        // If the bot leaves a voice channel, destroy player.
        if (event.getMember().getUser().equals(event.getJDA().getSelfUser())) {
            controllerManager.destroyPlayer(context);
            return;
        }

        if (!event.getMember().getUser().equals(event.getJDA().getSelfUser())
                && event.getChannelLeft().getMembers().size() == 1
                && event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember())) {
            controllerManager.destroyPlayer(context);

            /* TODO:
                Wait in VC alone for some time before disconnecting,
                but let the bot be able to start playing in a new channel if asked to while being alone in a VC.
                Look into: controllerManager.waitInVC(context);
            */

            return;
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        // Delete guild from database
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", event.getGuild().getId());
        }};

        String post = WebReq.Post("/servers/deleteGuild", data);
        Response r = gson.fromJson(post, Response.class);

        if (r.getCode() == StatusCodes.OK.getCode()) {
            log.info("[{}] Server deleted.", event.getGuild().getName());
        } else {
            log.error("[{}] Server could not be deleted.", event.getGuild().getName());
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        // Add guild in database
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", event.getGuild().getId());
            put("ownerId", event.getGuild().getOwnerId());
            put("name", event.getGuild().getName());
            put("prefix", System.getProperty("prefix"));
        }};

        String post = WebReq.Post("/servers/create", data);
        Response r = gson.fromJson(post, Response.class);

        if (r.getCode() == StatusCodes.OK.getCode()) {
            log.info("[{}] Server created.", event.getGuild().getName());
        } else {
            log.error("[{}] Server could not be created.", event.getGuild().getName());
        }
    }

    @Override
    public void onGuildUpdateName(@NotNull GuildUpdateNameEvent event) {
        // Update guild name in database
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", event.getGuild().getId());
            put("name", event.getGuild().getName());
        }};

        String post = WebReq.Post("/servers/updateGuildName", data);
        Response r = gson.fromJson(post, Response.class);

        if (r.getCode() == StatusCodes.OK.getCode()) {
            log.info("[{}] Updated name.", event.getGuild().getName());
        } else {
            log.error("[{}] Could not update name.", event.getGuild().getName());
        }
    }

    @Override
    public void onGuildUpdateOwner(@NotNull GuildUpdateOwnerEvent event) {
        // Update guild owner in database
        HashMap<String, ?> data = new HashMap<>() {{
            put("guildId", event.getGuild().getId());
            put("ownerId", event.getGuild().getOwnerId());
        }};

        String post = WebReq.Post("/servers/updateGuildOwner", data);
        Response r = gson.fromJson(post, Response.class);

        if (r.getCode() == StatusCodes.OK.getCode()) {
            log.info("[{}] Updated ownerId.", event.getGuild().getName());
        } else {
            log.error("[{}] Could not update ownerId.", event.getGuild().getName());
        }
    }
}
