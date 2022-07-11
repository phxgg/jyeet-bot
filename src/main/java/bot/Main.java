package bot;

import bot.api.StatusCodes;
import bot.api.WebReq;
import bot.dto.Response;
import bot.dto.Server;
import com.google.gson.Gson;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.security.auth.login.LoginException;

public class Main {
    public static void main(String[] args) {
        try {
            // Setup JDA
            JDA jda = JDABuilder.createDefault(System.getProperty("botToken"))
                    .setActivity(Activity.listening("music \uD83C\uDFB6"))
                    .addEventListeners(new BotApplicationManager())
                    .build();

            jda.awaitReady();

            Gson gson = new Gson();

            // Get all servers GET request
            String post = WebReq.Get("/servers/all");
            Response r = gson.fromJson(post, Response.class);

            if (r.getCode() == StatusCodes.OK.getCode()) {
                Server[] servers = gson.fromJson(gson.toJson(r.getData()), Server[].class);
                for (Server server : servers) {
                    System.out.println(server.getGuildId() + " " + server.getName());
                }
            }

            System.out.println("Logged in as " + jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator());
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
