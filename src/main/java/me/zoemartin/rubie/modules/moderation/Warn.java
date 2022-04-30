package me.zoemartin.rubie.modules.moderation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.persistence.criteria.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Disabled
@Command
@CommandOptions(
    name = "warn",
    description = "Warn a user",
    usage = "<user> <reason>",
    perm = CommandPerm.BOT_MODERATOR
)
public class Warn extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        Check.check(event.getArgs().size() > 1 && Parser.User.isParsable(event.getArgs().get(0)),
            CommandArgumentException::new);
        String userId = event.getArgs().get(0);

        User u = Bot.getJDA().getUserById(Parser.User.parse(userId));
        Check.notNull(u, UserNotFoundException::new);

        String reason = lastArg(1, event);

        ModLogEntity warnEntity = new ModLogEntity(
            event.getGuild().getId(), u.getId(), event.getMember().getId(), reason, Instant.now().toEpochMilli(),
            ModLogEntity.ModLogType.WARN);

        DatabaseUtil.saveObject(warnEntity);
        EmbedBuilder eb = new EmbedBuilder()
                              .setTitle("Warning added")
                              .setAuthor(String.format("%s / %s", u.getAsTag(), u.getId()), null, u.getEffectiveAvatarUrl())
                              .setDescription(String.format("Successfully warned %s for:\n\n%s", u.getAsMention(), warnEntity.getReason()));

        if (!u.isBot())
            u.openPrivateChannel().complete()
                .sendMessageFormat("You have received a warning from a Moderator on `%s`. \n**Reason**:\n\n%s",
                    event.getGuild().getName(), warnEntity.getReason()).queue();

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    @SubCommand(Warn.class)
    @CommandOptions(
        name = "list",
        description = "Lists a users warns",
        usage = "<user>",
        perm = CommandPerm.BOT_MODERATOR
    )
    private static class list extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() == 1 && Parser.User.isParsable(event.getArgs().get(0)), CommandArgumentException::new);
            String userId = event.getArgs().get(0);

            User u = Bot.getJDA().getUserById(Parser.User.parse(userId));
            Check.notNull(u, UserNotFoundException::new);

            Session s = DatabaseUtil.getSessionFactory().openSession();
            CriteriaBuilder cb = s.getCriteriaBuilder();

            CriteriaQuery<ModLogEntity> q = cb.createQuery(ModLogEntity.class);
            Root<ModLogEntity> r = q.from(ModLogEntity.class);
            List<ModLogEntity> warns = s.createQuery(q.select(r).where(
                cb.equal(r.get("type"), ModLogEntity.ModLogType.WARN.raw()),
                cb.equal(r.get("guild_id"), event.getGuild().getId()),
                cb.equal(r.get("user_id"), userId))).getResultList();

            List<MessageEmbed> pages = EmbedUtil.pagedFieldEmbed(
                new EmbedBuilder()
                    .setAuthor(String.format("%s / %s", u.getAsTag(), u.getId()),
                        null, u.getEffectiveAvatarUrl())
                    .setTitle("Warnings (" + warns.size() + ")").build(), warns.stream().map(e -> {
                    User moderator = Bot.getJDA().getUserById(e.getModerator_id());
                    return new MessageEmbed.Field("Warn ID: `" + e.getUuid() + "`",
                        String.format("**Responsible Moderator**: %s\n\n" +
                                          "**On**: %s\n\n" +
                                          "**Reason**: %s",
                            moderator != null ? moderator.getAsMention() : e.getModerator_id(),
                            new DateTime(e.getTimestamp(), DateTimeZone.UTC)
                                .toString("yyyy-MM-dd HH:mm:ss"),
                            e.getReason()), true);
                }).collect(Collectors.toList())
            );

            PageListener.add(new PagedEmbed(pages, event));
        }
    }

    @SubCommand(Warn.class)
    @CommandOptions(
        name = "remove",
        description = "Remove a warning",
        usage = "<uuid>",
        perm = CommandPerm.BOT_MODERATOR
    )
    private static class Remove extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() == 1, CommandArgumentException::new);

            UUID uuid = UUID.fromString(event.getArgs().get(0));

            Session s = DatabaseUtil.getSessionFactory().openSession();
            CriteriaBuilder cb = s.getCriteriaBuilder();

            CriteriaQuery<ModLogEntity> q = cb.createQuery(ModLogEntity.class);
            Root<ModLogEntity> r = q.from(ModLogEntity.class);
            List<ModLogEntity> warns = s.createQuery(q.select(r).where(
                cb.equal(r.get("type"), ModLogEntity.ModLogType.WARN.raw()),
                cb.equal(r.get("guild_id"), event.getGuild().getId()),
                cb.equal(r.get("uuid"), uuid))).getResultList();

            ModLogEntity warn = warns.isEmpty() ? null : warns.get(0);
            Check.notNull(warn, () -> new ReplyError("No warning with the ID `%s`", uuid));

            User u = Bot.getJDA().getUserById(warn.getUser_id());

            DatabaseUtil.deleteObject(warn);

            EmbedBuilder eb = new EmbedBuilder()
                                  .setTitle("Warning removed")
                                  .setDescription(String.format("Successfully removed warn `%s`", uuid));

            if (u != null)
                eb.setAuthor(String.format("%s / %s", u.getAsTag(), u.getId()), null, u.getEffectiveAvatarUrl());

            event.getChannel().sendMessageEmbeds(eb.build()).queue();
        }
    }

    @SubCommand(Warn.class)
    @CommandOptions(
        name = "import",
        description = "Bulk import user warns from an attachment",
        perm = CommandPerm.BOT_ADMIN
    )
    private static class BulkImportFile extends GuildCommand {
        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);
            Check.check(event.getAttachments().size() == 1, CommandArgumentException::new);
            Message m = event.getChannel().sendMessage("Okay... this might take a while").complete();

            InputStreamReader ir;
            BufferedReader br;
            try {
                ir = new InputStreamReader(event.getAttachments().get(0).retrieveInputStream().get(1, TimeUnit.MINUTES));
                br = new BufferedReader(ir);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new UnexpectedError(e);
            }

            Session s = DatabaseUtil.getSessionFactory().openSession();
            CriteriaBuilder cb = s.getCriteriaBuilder();

            CriteriaQuery<ModLogEntity> q = cb.createQuery(ModLogEntity.class);
            Root<ModLogEntity> r = q.from(ModLogEntity.class);
            List<ModLogEntity> existing = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), event.getGuild().getId()))).getResultList();

            Type listType = new TypeToken<ArrayList<ModLogs.ModLogEntry>>() {
            }.getType();
            List<ModLogs.ModLogEntry> toImport = new Gson().fromJson(br, listType);

            String guildId = event.getGuild().getId();
            List<ModLogEntity> modlogs = toImport.stream().filter(e -> e.getAction().equals("warn"))
                                             .map(e ->
                                                      new ModLogEntity(guildId, e.getOffender_id(), e.getModerator_id(), e.getReason(),
                                                          DateTime.parse(e.getTimestamp(), TIME_FORMATTER).getMillis(), ModLogEntity.ModLogType.WARN)

                                             ).filter(e -> existing.stream().noneMatch(e::equals))
                                             .collect(Collectors.toList());

            modlogs.forEach(DatabaseUtil::saveObject);
            Set<String> users = modlogs.stream().map(ModLogEntity::getUser_id)
                                    .collect(Collectors.toCollection(HashSet::new));

            EmbedBuilder eb = new EmbedBuilder().setTitle("Bulk Warn Import");
            eb.setDescription("Imported warns:\n" + String.join("\n", users));
            m.delete().complete();
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
        }
    }
}
