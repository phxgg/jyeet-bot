package bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

public class Main extends ListenerAdapter {
    public static void main(String args[]) {
        final String botToken = "ODkxNzU4NzU2MzE1MTY0Njgy.YVDBDw.MC03pWhSReU5jTqv87ZkFpY3sdg";

        try {
            JDA jda = JDABuilder.createDefault(botToken)
                    .setActivity(Activity.playing("Your mom's clit"))
                    .addEventListeners(new BotApplicationManager())
                    .build();

            jda.awaitReady();

            System.out.println("Finished Building JDA!");
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
