package me.zoemartin.rubie.modules.hatchery;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MVCRefresh implements GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        Guild g = event.getGuild();
        Role mvc = g.getRolesByName("mvc", true).get(0);
        Role mvcOld = g.getRolesByName("mvc-old", true).get(0);

        Check.entityReferenceNotNull(mvc, Role.class, "mvc");
        Check.entityReferenceNotNull(mvcOld, Role.class, "mvc-old");

        List<Member> mvcMembers = g.getMembersWithRoles(mvc);
        g.getMembersWithRoles(mvcOld).forEach(member -> g.removeRoleFromMember(member, mvcOld).queue());
        mvcMembers.forEach(member -> {
            g.addRoleToMember(member, mvcOld).queue();
            g.removeRoleFromMember(member, mvc).queue();
        });

        embedReply(event, "MVC Refresh", "Refreshed MVC! Previous mvc count was %d",
            mvcMembers.size()).queue();
    }

    @NotNull
    @Override
    public String name() {
        return "mvcrefresh";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @NotNull
    @Override
    public String description() {
        return "Refreshes MVC";
    }
}
