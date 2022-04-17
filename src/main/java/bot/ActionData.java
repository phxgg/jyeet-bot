package bot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.managers.AudioManager;

public record ActionData(
        MessageDispatcher messageDispatcher,
        Member member,
        Guild guild,
        GuildChannel guildChannel,
        AudioManager audioManager
) {
}
