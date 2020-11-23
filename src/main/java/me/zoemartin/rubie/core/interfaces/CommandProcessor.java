package me.zoemartin.rubie.core.interfaces;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public interface CommandProcessor {
    void process(MessageReceivedEvent event, String input);
}
