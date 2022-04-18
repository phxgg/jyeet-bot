package bot.records;

import bot.MessageDispatcher;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.managers.AudioManager;

public class ActionData {
    private final MessageDispatcher messageDispatcher;
    private final Member member;
    private final Guild guild;
    private final GuildChannel guildChannel;
    private final AudioManager audioManager;

    public ActionData(
            MessageDispatcher messageDispatcher,
            Member member,
            Guild guild,
            GuildChannel guildChannel,
            AudioManager audioManager) {
        this.messageDispatcher = messageDispatcher;
        this.member = member;
        this.guild = guild;
        this.guildChannel = guildChannel;
        this.audioManager = audioManager;
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public Member getMember() {
        return member;
    }

    public Guild getGuild() {
        return guild;
    }

    public GuildChannel getGuildChannel() {
        return guildChannel;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }
}
