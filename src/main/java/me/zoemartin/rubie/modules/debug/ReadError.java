package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import me.zoemartin.rubie.modules.commandProcessing.LoggedError;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.hibernate.Session;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.UUID;

public class ReadError implements GuildCommand {
    @Override
    public String name() {
        return "readerror";
    }

    @Override
    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        Check.check(args.size() == 1, CommandArgumentException::new);

        UUID uuid = UUID.fromString(args.get(0));

        Session s = DatabaseUtil.getSessionFactory().openSession();
        CriteriaBuilder cb = s.getCriteriaBuilder();

        CriteriaQuery<LoggedError> q = cb.createQuery(LoggedError.class);
        Root<LoggedError> r = q.from(LoggedError.class);
        List<LoggedError> errors = s.createQuery(q.select(r).where(
            cb.equal(r.get("uuid"), uuid))).getResultList();

        LoggedError error = errors.isEmpty() ? null : errors.get(0);
        Check.notNull(error, () -> new ReplyError("No error with the ID `%s`", uuid));

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("Error Debug")
            .setDescription("```" + error.getError_message() + "\n\n" +
                                error.getError_stacktrace().substring(1, error.getError_stacktrace().length() - 1)
                                + "```")
            .addField("Guild", Bot.getJDA().getGuildById(error.getGuild_id()).toString(), true)
            .addField("Invoked by", error.getInvoked_message(), true);

        channel.sendMessage(eb.build()).queue();
    }

    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.OWNER;
    }

    @Override
    public String usage() {
        return "readerror <uuid>";
    }

    @Override
    public String description() {
        return "read an error";
    }
}
