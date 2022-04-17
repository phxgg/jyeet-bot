package bot;

import bot.music.TrackBoxButtonClick;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;

public class Main {
    public static void main(String args[]) {
        try {
            // Setup JDA
            JDA jda = JDABuilder.createDefault(System.getProperty("botToken"))
                    .setActivity(Activity.listening("ur mom \uD83C\uDFB6"))
                    .addEventListeners(new BotApplicationManager())
                    .build();

            jda.awaitReady();

            System.out.println("Logged in as " + jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator());
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
