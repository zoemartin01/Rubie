package me.zoemartin.rubie.modules.moderation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.Export.Notes;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import javax.persistence.criteria.*;
import java.io.*;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Note implements GuildCommand {
    @Override
    public @NotNull Set<Command> subCommands() {
        return Set.of(new list(), new Remove(), new BulkImportFile(), new Clear(), new Add());
    }

    @Override
    public @NotNull String name() {
        return "notes";
    }

    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        new list().run(user, channel, args, original, "list");
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_MODERATOR;
    }

    @NotNull
    @Override
    public String usage() {
        return "<user>";
    }

    @Override
    public @NotNull String description() {
        return "Notes";
    }

    private static class Add implements GuildCommand {

        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            Check.check(args.size() > 1, CommandArgumentException::new);
            String userId = null;
            User u = null;
            String arg;
            if (Parser.User.isParsable(arg = args.get(0))) {
                u = CacheUtils.getUser(arg);
                userId = u == null ? Parser.User.parse(arg) : u.getId();
            } else if (Parser.User.tagIsParsable(arg)) {
                u = Bot.getJDA().getUserByTag(arg);
                userId = u == null ? null : u.getId();
            }

            Check.notNull(userId, () -> new EntityNotFoundException("Can't find user `%s`", arg));

            String note = lastArg(1, args, original);

            NoteEntity noteEntity = new NoteEntity(
                original.getGuild().getId(), userId, user.getId(), note,
                original.getTimeCreated().toInstant().toEpochMilli());

            DatabaseUtil.saveObject(noteEntity);
            EmbedBuilder eb = new EmbedBuilder()
                                  .setTitle("Note added")
                                  .setAuthor(u == null ? userId : String.format("%s / %s", u.getAsTag(), u.getId()), null,
                                      u == null ? null : u.getEffectiveAvatarUrl())
                                  .setDescription(String.format("Successfully added a note to %s:\n\n%s",
                                      u == null ? userId : u.getAsMention(),
                                      noteEntity.getNote()));

            channel.sendMessage(eb.build()).queue();
        }

        @NotNull
        @Override
        public String name() {
            return "add";
        }

        @Override
        @NotNull
        public String usage() {
            return "<user> <note>";
        }


        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MODERATOR;
        }

        @NotNull
        @Override
        public String description() {
            return "Adds a note to a user";
        }
    }

    private static class list implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "list";
        }

        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            Check.check(!args.isEmpty(), CommandArgumentException::new);
            String userId = null;
            User u = null;
            String arg;
            if (Parser.User.isParsable(arg = args.get(0))) {
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
                cb.equal(r.get("guild_id"), original.getGuild().getId()),
                cb.equal(r.get("user_id"), userId))).getResultList();

            List<MessageEmbed> pages = EmbedUtil.pagedFieldEmbed(
                new EmbedBuilder()
                    .setAuthor(u == null ? userId : String.format("%s / %s", u.getAsTag(), u.getId()),
                        null, u == null ? null : u.getEffectiveAvatarUrl())
                    .setTitle("Notes (" + notes.size() + ")").build(), notes.stream().map(e -> {
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

            PageListener.add(new PagedEmbed(pages, channel, user.getUser()));
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_MODERATOR;
        }

        @Override
        public @NotNull String usage() {
            return "<user>";
        }

        @Override
        public @NotNull String description() {
            return "Lists a users notes";
        }
    }

    private static class Remove implements GuildCommand {
        @Override
        public @NotNull String name() {
            return "remove";
        }

        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            Check.check(args.size() == 1, CommandArgumentException::new);

            UUID uuid = UUID.fromString(args.get(0));

            Session s = DatabaseUtil.getSessionFactory().openSession();
            CriteriaBuilder cb = s.getCriteriaBuilder();

            CriteriaQuery<NoteEntity> q = cb.createQuery(NoteEntity.class);
            Root<NoteEntity> r = q.from(NoteEntity.class);
            List<NoteEntity> notes = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), original.getGuild().getId()),
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

            channel.sendMessage(eb.build()).queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @Override
        public @NotNull String usage() {
            return "<uuid>";
        }

        @Override
        public @NotNull String description() {
            return "Remove a warning";
        }
    }

    private static class Clear implements GuildCommand {

        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            Check.check(!args.isEmpty(), CommandArgumentException::new);
            String userId = null;
            User u = null;
            String arg;
            if (Parser.User.isParsable(arg = args.get(0))) {
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
                cb.equal(r.get("guild_id"), original.getGuild().getId()),
                cb.equal(r.get("user_id"), userId))).getResultList();

            notes.forEach(DatabaseUtil::deleteObject);
            embedReply(original, channel, "Notes", "Cleared all notes for %s",
                u == null ? userId : u.getAsMention()).queue();
        }

        @NotNull
        @Override
        public String name() {
            return "clear";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @NotNull
        @Override
        public String description() {
            return "Clears a users notes";
        }
    }

    private static class BulkImportFile implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "import";
        }

        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            Check.check(args.isEmpty(), CommandArgumentException::new);
            Check.check(original.getAttachments().size() == 1, CommandArgumentException::new);
            Message m = channel.sendMessage("Okay... this might take a while").complete();

            InputStreamReader ir;
            BufferedReader br;
            try {
                ir = new InputStreamReader(original.getAttachments().get(0).retrieveInputStream().get(1, TimeUnit.MINUTES));
                br = new BufferedReader(ir);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new UnexpectedError(e);
            }

            Session s = DatabaseUtil.getSessionFactory().openSession();
            CriteriaBuilder cb = s.getCriteriaBuilder();

            CriteriaQuery<NoteEntity> q = cb.createQuery(NoteEntity.class);
            Root<NoteEntity> r = q.from(NoteEntity.class);
            List<NoteEntity> existing = s.createQuery(q.select(r).where(
                cb.equal(r.get("guild_id"), original.getGuild().getId()))).getResultList();

            Type listType = new TypeToken<ArrayList<Notes.NoteEntry>>(){}.getType();
            List<Notes.NoteEntry> toImport = new Gson().fromJson(br, listType);

            String guildId = original.getGuild().getId();
            List<NoteEntity> notes = toImport.stream()
                                         .map(e -> new NoteEntity(
                                             guildId, e.getUser_id(), e.getModerator_id(), e.getNote(),
                                             DateTime.parse(e.getTimestamp(), ISODateTimeFormat.dateTime()).getMillis()))
                                         .filter(e -> existing.stream().noneMatch(e::equals))
                                         .collect(Collectors.toList());

            notes.forEach(DatabaseUtil::saveObject);
            Set<String> users = notes.stream().map(NoteEntity::getUser_id).collect(Collectors.toCollection(HashSet::new));

            EmbedBuilder eb = new EmbedBuilder().setTitle("Bulk Note Import");
            eb.setDescription("Imported Notes:\n" + String.join("\n", users));
            m.delete().complete();
            channel.sendMessage(eb.build()).queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Bulk Import Notes. Attach a text file with one Line for each warn.";
        }
    }
}
