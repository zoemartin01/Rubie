package me.zoemartin.rubie.core.interfaces;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface CommandProcessor {
    void process(MessageReceivedEvent event, String input);
}
