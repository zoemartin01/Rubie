package me.zoemartin.rubie.core.interfaces;

import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.Checks;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.util.Help;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Arrays;

public abstract class GuildCommand extends AbstractCommand {
    public GuildCommand() {
        super();
    }

    public GuildCommand(CommandConfiguration configuration) {
        super(configuration);
    }

    public boolean checkChannelPerms(GuildCommandEvent event) {
        return checkChannelPerms(event, event.getTextChannel());
    }

    public boolean checkChannelPerms(GuildCommandEvent event, TextChannel custom) {
        if (event.getMember() == null || event.getGuild() == null || custom == null) return false;
        Checks.Permissions.Channel[] options = this.getClass().getAnnotationsByType(Checks.Permissions.Channel.class);

        return Arrays.stream(options)
                   .allMatch(o -> event.getMember().hasPermission(custom, o.value()));
    }

    public boolean checkGuildPerms(GuildCommandEvent event) {
        if (event.getMember() == null || event.getGuild() == null) return false;
        Checks.Permissions.Guild[] options = this.getClass().getAnnotationsByType(Checks.Permissions.Guild.class);

        return Arrays.stream(options)
                   .allMatch(o -> event.getMember().hasPermission(event.getTextChannel(), o.value()));
    }

    public boolean checkNecessaryPerms(GuildCommandEvent event) {
        return checkNecessaryPerms(event, event.getTextChannel());
    }

    public boolean checkNecessaryPerms(GuildCommandEvent event, TextChannel custom) {
        if (event.getGuild() == null || event.getTextChannel() == null || custom == null) return false;
        CommandOptions[] options = this.getClass().getAnnotationsByType(CommandOptions.class);

        return Arrays.stream(options)
                   .allMatch(o -> event.getGuild().getSelfMember().hasPermission(custom, o.botPerms()));
    }

    public abstract void run(GuildCommandEvent event);

    @Override
    public void run(CommandEvent event) {
        if (event instanceof GuildCommandEvent) {
            run((GuildCommandEvent) event);
        } else {
            throw new IllegalStateException("Command Event not from a Guild!");
        }
    }

    public void sendHelp(GuildCommandEvent event) {
        if (Help.getHelper() != null) Help.getHelper().send(event);
    }
}
