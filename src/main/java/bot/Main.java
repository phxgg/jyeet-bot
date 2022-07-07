package bot;

import bot.api.WebReq;
import bot.dto.Response;
import bot.dto.Server;
import com.google.gson.Gson;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        try {
            // Setup JDA
            JDA jda = JDABuilder.createDefault(System.getProperty("botToken"))
                    .setActivity(Activity.listening("ur mom \uD83C\uDFB6"))
                    .addEventListeners(new BotApplicationManager())
                    .build();

            jda.awaitReady();

            Gson gson = new Gson();

            HashMap<String, ?> d = new HashMap<>() {{
                put("guildId", 123456711);
                put("ownerId", 23444);
                put("name", "Test Server");
            }};

            String p = WebReq.Post("/servers/create", d);
            Response i = gson.fromJson(p, Response.class);
            System.out.println(p);

            String r = WebReq.Get("/servers/all");
            Response o = gson.fromJson(r, Response.class);

            Server[] servers = gson.fromJson(gson.toJson(o.getData()), Server[].class);
            for (Server server : servers) {
                System.out.println(server.getGuildId() + " " + server.getName());
            }

            System.out.println("Logged in as " + jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator());
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
