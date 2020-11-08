package me.zoemartin.rubie.modules.hatchery;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MVC implements GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        Guild g = event.getGuild();
        g.loadMembers().get();

        int abstains = 0;
        if (!event.getArgs().isEmpty()) abstains = Parser.Int.parse(event.getArgs().get(0));

        Role mvc = g.getRolesByName("mvc", true).get(0);
        Check.entityReferenceNotNull(mvc, Role.class, "mvc");


        int count = g.getMembersWithRoles(mvc).size();
        int majority = (count - abstains) / 2 + 1;

        embedReply(event, "MVC Majority", "The majority of %d with %d abstains is %d",
            count, abstains, majority).queue();
    }

    @NotNull
    @Override
    public String name() {
        return "mvc";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @NotNull
    @Override
    public String usage() {
        return "[abstains]";
    }

    @NotNull
    @Override
    public String description() {
        return "Returns the MVC Count";
    }
}
