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
import org.joda.time.format.*;

import javax.persistence.criteria.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Disabled
@Command
@CommandOptions(
    name = "modlogs",
    description = "Lists a user's modlogs",
    usage = "<user>",
    perm = CommandPerm.BOT_MODERATOR
)
public class ModLogs extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        new list().run(event);
    }

    @SubCommand(ModLogs.class)
    @CommandOptions(
        name = "list",
        description = "Lists a user's modlogs",
        usage = "<user>",
        perm = CommandPerm.BOT_MODERATOR
    )
    private static class list extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
            String userId = null;
            User u = null;
            String arg;
            if (Parser.User.isParsable(arg = event.getArgs().get(0))) {
                u = CacheUtils.getUser(arg);
                userId = u == null ? Parser.User.parse(arg) : u.getId();
            } else if (Parser.User.tagIsParsable(arg)) {
                u = Bot.getJDA().getUserByTag(arg);
                userId = u == null ? null : u.getId();
            }

            Check.notNull(userId, () -> new EntityNotFoundException("Can't find user `%s`", arg));

            Session s = DatabaseUtil.getSessionFactory().openSession();
            CriteriaBuilder cb = s.getCriteriaBuilder();

            CriteriaQuery<ModLogEntity> q = cb.createQuery(ModLogEntity.class);
            Root<ModLogEntity> r = q.from(ModLogEntity.class);
            List<ModLogEntity> modlogs = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), event.getGuild().getId()),
                cb.equal(r.get("user_id"), userId))).getResultList();

            List<MessageEmbed> pages = EmbedUtil.pagedFieldEmbed(
                new EmbedBuilder()
                    .setAuthor(u == null ? userId : String.format("%s / %s", u.getAsTag(), u.getId()),
                        null, u == null ? null : u.getEffectiveAvatarUrl())
                    .setTitle("Modlogs (" + modlogs.size() + ")").build(), modlogs.stream().map(e -> {
                    User moderator = e.getModerator_id() == null ? null :
                                         Bot.getJDA().getUserById(e.getModerator_id());
                    return new MessageEmbed.Field(e.getType().name() + " | Modlog ID: `" + e.getUuid() + "`",
                        String.format("**Responsible Moderator**: %s\n\n" +
                                          "**On**: %s\n\n" +
                                          "**Reasons**: %s",
                            moderator != null ? moderator.getAsMention() : e.getModerator_id(),
                            new DateTime(e.getTimestamp(), DateTimeZone.UTC)
                                .toString("yyyy-MM-dd HH:mm:ss"),
                            e.getReason() == null ? "n/a" : e.getReason()), true);
                }).collect(Collectors.toList()), 1000
            );

            PageListener.add(new PagedEmbed(pages, event));
        }
    }

    @SubCommand(ModLogs.class)
    @CommandOptions(
        name = "remove",
        description = "Remove a user's modlog",
        usage = "<uuid>",
        perm = CommandPerm.BOT_ADMIN
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
            List<ModLogEntity> notes = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), event.getGuild().getId()),
                cb.equal(r.get("uuid"), uuid))).getResultList();

            ModLogEntity modlog = notes.isEmpty() ? null : notes.get(0);
            Check.notNull(modlog, () -> new ReplyError("No modlog with the ID `%s`", uuid));

            User u = Bot.getJDA().getUserById(modlog.getUser_id());

            DatabaseUtil.deleteObject(modlog);

            EmbedBuilder eb = new EmbedBuilder()
                                  .setTitle("Modlog removed")
                                  .setDescription(String.format("Successfully removed modlog `%s`", uuid));

            if (u != null)
                eb.setAuthor(String.format("%s / %s", u.getAsTag(), u.getId()), null, u.getEffectiveAvatarUrl());

            event.getChannel().sendMessage(eb.build()).queue();
        }
    }

    @SubCommand(ModLogs.class)
    @CommandOptions(
        name = "clear",
        description = "Clears a user's modlogs",
        usage = "<user>",
        perm = CommandPerm.BOT_ADMIN
    )
    private static class Clear extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            String userId = null;
            User u = null;
            String arg;
            if (Parser.User.isParsable(arg = event.getArgs().get(0))) {
                u = CacheUtils.getUser(arg);
                userId = u == null ? Parser.User.parse(arg) : u.getId();
            } else if (Parser.User.tagIsParsable(arg)) {
                u = Bot.getJDA().getUserByTag(arg);
                userId = u == null ? null : u.getId();
            }

            Check.notNull(userId, () -> new EntityNotFoundException("Can't find user `%s`", arg));

            Session s = DatabaseUtil.getSessionFactory().openSession();
            CriteriaBuilder cb = s.getCriteriaBuilder();

            CriteriaQuery<ModLogEntity> q = cb.createQuery(ModLogEntity.class);
            Root<ModLogEntity> r = q.from(ModLogEntity.class);
            List<ModLogEntity> modlog = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), event.getGuild().getId()),
                cb.equal(r.get("user_id"), userId))).getResultList();

            modlog.forEach(DatabaseUtil::deleteObject);
            embedReply(event, "Notes", "Cleared all modlogs for %s",
                u == null ? userId : u.getAsMention()).queue();
        }
    }

    @SubCommand(ModLogs.class)
    @CommandOptions(
        name = "import",
        description = "Bulk import user modlogs from an attachment",
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

            Type listType = new TypeToken<ArrayList<ModLogEntry>>() {
            }.getType();
            List<ModLogEntry> toImport = new Gson().fromJson(br, listType);

            String guildId = event.getGuild().getId();
            List<ModLogEntity> modlogs = toImport.stream()
                                             .map(e -> {
                                                     ModLogEntity.ModLogType type = switch (e.getAction()) {
                                                         case "warn" -> ModLogEntity.ModLogType.WARN;
                                                         case "ban" -> ModLogEntity.ModLogType.BAN;
                                                         case "mute" -> ModLogEntity.ModLogType.MUTE;
                                                         case "unban" -> ModLogEntity.ModLogType.UNBAN;
                                                         case "unmute" -> ModLogEntity.ModLogType.UNMUTE;
                                                         case "kick" -> ModLogEntity.ModLogType.KICK;
                                                         default -> ModLogEntity.ModLogType.NONE;
                                                     };

                                                     return new ModLogEntity(guildId, e.offender_id, e.moderator_id, e.reason,
                                                         DateTime.parse(e.getTimestamp(), TIME_FORMATTER).getMillis(), type);
                                                 }
                                             ).filter(e -> existing.stream().noneMatch(e::equals))
                                             .collect(Collectors.toList());

            modlogs.forEach(DatabaseUtil::saveObject);
            Set<String> users = modlogs.stream().map(ModLogEntity::getUser_id)
                                    .collect(Collectors.toCollection(HashSet::new));

            EmbedBuilder eb = new EmbedBuilder().setTitle("Bulk Note Import");
            eb.setDescription("Imported Notes:\n" + String.join("\n", users));
            m.delete().complete();
            event.getChannel().sendMessage(eb.build()).queue();
        }
    }

    static class ModLogEntry implements Serializable {
        private String moderator_id;

        private String offender_id;

        private String action;

        private String timestamp;

        private String reason = null;

        public ModLogEntry(String moderator_id, String offender_id, String action, String timestamp) {
            this.moderator_id = moderator_id;
            this.offender_id = offender_id;
            this.action = action;
            this.timestamp = timestamp;
        }

        public ModLogEntry(String moderator_id, String offender_id, String action, String timestamp, String reason) {
            this.moderator_id = moderator_id;
            this.offender_id = offender_id;
            this.action = action;
            this.timestamp = timestamp;
            this.reason = reason;
        }

        public String getModerator_id() {
            return moderator_id;
        }

        public String getOffender_id() {
            return offender_id;
        }

        public String getAction() {
            return action;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getReason() {
            return reason;
        }
    }
}
