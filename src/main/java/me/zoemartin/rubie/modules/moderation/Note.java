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
import me.zoemartin.rubie.modules.Export.Notes;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import javax.persistence.criteria.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "notes",
    description = "Lists a user's notes",
    usage = "<user>",
    perm = CommandPerm.BOT_MODERATOR
)
public class Note extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        new list().run(event);
    }

    @SubCommand(Note.class)
    @CommandOptions(
        name = "add",
        description = "Adds a note to a user",
        usage = "<user> <note>",
        perm = CommandPerm.BOT_MODERATOR
    )
    @SubCommand.AsBase(name = "addnote", alias = "setnote")
    private static class Add extends GuildCommand {

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() > 1, CommandArgumentException::new);
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

            String note = lastArg(1, event);

            NoteEntity noteEntity = new NoteEntity(
                event.getGuild().getId(), userId, event.getUser().getId(), note,
                Instant.now().toEpochMilli());

            DatabaseUtil.saveObject(noteEntity);
            EmbedBuilder eb = new EmbedBuilder()
                                  .setTitle("Note added")
                                  .setAuthor(u == null ? userId : String.format("%s / %s", u.getAsTag(), u.getId()), null,
                                      u == null ? null : u.getEffectiveAvatarUrl())
                                  .setDescription(String.format("Successfully added a note to %s:\n\n%s",
                                      u == null ? userId : u.getAsMention(),
                                      noteEntity.getNote()));

            event.getChannel().sendMessage(eb.build()).queue();
        }
    }

    @SubCommand(Note.class)
    @CommandOptions(
        name = "list",
        description = "Lists a user's notes",
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

            CriteriaQuery<NoteEntity> q = cb.createQuery(NoteEntity.class);
            Root<NoteEntity> r = q.from(NoteEntity.class);
            List<NoteEntity> notes = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), event.getGuild().getId()),
                cb.equal(r.get("user_id"), userId))).getResultList();

            var sorted = notes.stream().sorted(
                Comparator.comparingLong(NoteEntity::getTimestamp).reversed());

            List<MessageEmbed> pages = EmbedUtil.pagedFieldEmbed(
                new EmbedBuilder()
                    .setAuthor(u == null ? userId : String.format("%s / %s", u.getAsTag(), u.getId()),
                        null, u == null ? null : u.getEffectiveAvatarUrl())
                    .setTitle("Notes (" + notes.size() + ")").build(), sorted.map(e -> {
                    User moderator = e.getModerator_id() == null ? null :
                                         Bot.getJDA().getUserById(e.getModerator_id());
                    return new MessageEmbed.Field("Note ID: `" + e.getUuid() + "`",
                        String.format("**Responsible Moderator**: %s\n\n" +
                                          "**On**: %s\n\n" +
                                          "**Note**: %s",
                            moderator != null ? moderator.getAsMention() : e.getModerator_id(),
                            new DateTime(e.getTimestamp(), DateTimeZone.UTC)
                                .toString("yyyy-MM-dd HH:mm:ss"),
                            e.getNote()), true);
                }).collect(Collectors.toList()), 1000
            );

            PageListener.add(new PagedEmbed(pages, event));
        }
    }

    @SubCommand(Note.class)
    @CommandOptions(
        name = "remove",
        description = "Remove a user's note",
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

            CriteriaQuery<NoteEntity> q = cb.createQuery(NoteEntity.class);
            Root<NoteEntity> r = q.from(NoteEntity.class);
            List<NoteEntity> notes = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), event.getGuild().getId()),
                cb.equal(r.get("uuid"), uuid))).getResultList();

            NoteEntity note = notes.isEmpty() ? null : notes.get(0);
            Check.notNull(note, () -> new ReplyError("No note with the ID `%s`", uuid));

            User u = Bot.getJDA().getUserById(note.getUser_id());

            DatabaseUtil.deleteObject(note);

            EmbedBuilder eb = new EmbedBuilder()
                                  .setTitle("Note removed")
                                  .setDescription(String.format("Successfully removed note `%s`", uuid));

            if (u != null)
                eb.setAuthor(String.format("%s / %s", u.getAsTag(), u.getId()), null, u.getEffectiveAvatarUrl());

            event.getChannel().sendMessage(eb.build()).queue();
        }
    }

    @SubCommand(Note.class)
    @CommandOptions(
        name = "clear",
        description = "Clear a user's notes",
        usage = "<user>",
        perm = CommandPerm.BOT_MODERATOR
    )
    private static class Clear extends GuildCommand {
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

            CriteriaQuery<NoteEntity> q = cb.createQuery(NoteEntity.class);
            Root<NoteEntity> r = q.from(NoteEntity.class);
            List<NoteEntity> notes = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), event.getGuild().getId()),
                cb.equal(r.get("user_id"), userId))).getResultList();

            notes.forEach(DatabaseUtil::deleteObject);
            event.reply("Notes", "Cleared all notes for %s",
                u == null ? userId : u.getAsMention()).queue();
        }
    }

    @SubCommand(Note.class)
    @CommandOptions(
        name = "import",
        description = "Bulk import user notes from an attachment",
        perm = CommandPerm.BOT_ADMIN
    )
    private static class BulkImportFile extends GuildCommand {
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

            CriteriaQuery<NoteEntity> q = cb.createQuery(NoteEntity.class);
            Root<NoteEntity> r = q.from(NoteEntity.class);
            List<NoteEntity> existing = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), event.getGuild().getId()))).getResultList();

            Type listType = new TypeToken<ArrayList<Notes.NoteEntry>>() {
            }.getType();
            List<Notes.NoteEntry> toImport = new Gson().fromJson(br, listType);

            String guildId = event.getGuild().getId();
            List<NoteEntity> notes = toImport.stream()
                                         .map(e -> new NoteEntity(
                                             guildId, e.getUser_id(), e.getModerator_id(), e.getNote(),
                                             DateTime.parse(e.getTimestamp(), ISODateTimeFormat.dateTime()).getMillis()))
                                         .filter(e -> existing.stream().noneMatch(e::equals))
                                         .collect(Collectors.toList());

            notes.forEach(DatabaseUtil::saveObject);
            Set<String> users = notes.stream().map(NoteEntity::getUser_id).collect(Collectors.toCollection(HashSet::new));

            EmbedBuilder eb = new EmbedBuilder().setTitle("Bulk Note Import");
            var paged = new PagedEmbed(EmbedUtil.pagedDescription(eb.build(), users.stream().map(s1 -> s1 + "\n")
                                                                                  .collect(Collectors.toList())), event);
            PageListener.add(paged);
            m.delete().complete();
        }
    }
}
