package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import me.zoemartin.rubie.modules.commandProcessing.LoggedError;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import org.hibernate.Session;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.UUID;

@Command
@CommandOptions(
    name = "readerror",
    description = "Output information about a logged Error",
    usage = "<uuid>",
    perm = CommandPerm.OWNER
)
public class ReadError extends AbstractCommand {

    @Override
    public void run(CommandEvent event) {
        Check.check(event.getArgs().size() == 1, CommandArgumentException::new);

        UUID uuid = UUID.fromString(event.getArgs().get(0));

        Session s = DatabaseUtil.getSessionFactory().openSession();
        CriteriaBuilder cb = s.getCriteriaBuilder();

        CriteriaQuery<LoggedError> q = cb.createQuery(LoggedError.class);
        Root<LoggedError> r = q.from(LoggedError.class);
        List<LoggedError> errors = s.createQuery(q.select(r).where(
            cb.equal(r.get("uuid"), uuid))).getResultList();

        LoggedError error = errors.isEmpty() ? null : errors.get(0);
        Check.notNull(error, () -> new ReplyError("No error with the ID `%s`", uuid));

        Guild g;
        if (error.getGuild_id() == null) g = null;
        else g = event.getJDA().getGuildById(error.getGuild_id());

        EmbedBuilder eb = new EmbedBuilder()
                              .setTitle("Error Debug")
                              .setDescription("```" + error.getError_message() + "\n\n" +
                                                  error.getError_stacktrace().substring(1, error.getError_stacktrace().length() - 1)
                                                  + "```")
                              .addField("Guild", g == null ? "n/a" : g.toString(), true)
                              .addField("Invoked by", error.getInvoked_message(), true);

        event.getChannel().sendMessage(eb.build()).queue();
    }
}
