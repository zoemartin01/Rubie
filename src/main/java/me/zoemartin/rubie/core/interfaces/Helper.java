package me.zoemartin.rubie.core.interfaces;

import net.dv8tion.jda.api.entities.*;

import java.util.List;

public interface Helper {
    void send(User user, MessageChannel channel, List<String> args, Message original, String invoked);
}
