package me.zoemartin.rubie.core.interfaces;

import me.zoemartin.rubie.core.GuildCommandEvent;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.List;

public interface Helper {
    @Deprecated
    default void send(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        throw new IllegalAccessError("Deprecated");
    }

    void send(GuildCommandEvent event);
}
