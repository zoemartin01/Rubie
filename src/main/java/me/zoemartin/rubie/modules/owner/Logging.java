package me.zoemartin.rubie.modules.owner;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Logging extends ListenerAdapter {
    static final String JOIN_LOGS = "782982176031375451";

    private static final Logger log = LoggerFactory.getLogger(Logging.class);

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        new Thread(join(event)).start();
        log.info("Joined a new Guild {}", event.getGuild());
    }

    @SuppressWarnings("ConstantConditions")
    public Runnable join(GuildJoinEvent event) {
        return () -> {
            var eb = new EmbedBuilder();
            var g = event.getGuild();

            eb.setTitle("Joined a new Guild");
            eb.setDescription(String.format("%s / %s", g.getName(), g.getId()));
            var owner = g.retrieveOwner().complete();
            eb.addField("Owner", String.format("%s / %s / %s", owner.getAsMention(), owner.getUser().getAsTag(),
                owner.getId()), false);
            eb.addField("Members", String.valueOf(g.loadMembers().get().size()), false);

            event.getJDA().getGuildById(Todo.DEV_SERVER_ID).getTextChannelById(JOIN_LOGS).sendMessage(eb.build()).queue();
        };
    }
}
