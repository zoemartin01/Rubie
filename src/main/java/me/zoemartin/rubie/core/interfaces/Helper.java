package me.zoemartin.rubie.core.interfaces;

import me.zoemartin.rubie.core.GuildCommandEvent;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.List;

public interface Helper {
    void send(GuildCommandEvent event);
}
