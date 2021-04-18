package me.zoemartin.rubie.modules.logging;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.DatabaseEntry;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@DatabaseEntity
@Entity
@Table(name = "usernames")
public class Username implements DatabaseEntry {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID key;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "name")
    private String name;

    @Column(name = "timestamp")
    private long timestamp;

    public Username() {
    }

    public Username(User user) {
        this.userId = user.getId();
        this.name = user.getName();
        this.timestamp = Instant.now().toEpochMilli();
    }

    public Username(User user, String name) {
        this.userId = user.getId();
        this.name = name;
        this.timestamp = Instant.now().toEpochMilli() - 1;
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    static class UsernameListener extends ListenerAdapter {
        @Override
        public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
            var s = DatabaseUtil.getSessionFactory().openSession();
            var cb = s.getCriteriaBuilder();

            var q = cb.createQuery(Username.class);
            var r = q.from(Username.class);
            var l = s.createQuery(q.select(r).where(
                cb.equal(r.get("userId"), event.getUser().getId()),
                cb.equal(r.get("name"), event.getOldName())
            )).getResultList();

            if (l.isEmpty())
                DatabaseUtil.saveObject(new Username(event.getUser(), event.getOldName()));
            DatabaseUtil.saveObject(new Username(event.getUser()));
        }
    }

    @Command
    @CommandOptions(
        name = "usernames",
        description = "Shows a users past usernames",
        usage = "<user>",
        perm = CommandPerm.BOT_MANAGER
    )
    private static class ListNicknames extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            var args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);

            var ref = lastArg(0, event);
            MessageEmbed template;
            String userRef;

            if (Parser.User.isParsable(ref)) {
                userRef = Parser.User.parse(ref);
                var user = CacheUtils.getUser(userRef);

                if (user == null) template = new EmbedBuilder().setTitle("Usernames for `" + userRef + "`").build();
                else template = new EmbedBuilder()
                                    .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                                    .setTitle("Usernames")
                                    .build();

            } else if (Parser.User.tagIsParsable(ref)) {
                var user = event.getJDA().getUserByTag(ref);
                Check.entityReferenceNotNull(user, User.class, ref);

                userRef = user.getId();
                template = new EmbedBuilder()
                               .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                               .setTitle("Usernames")
                               .build();
            } else throw new ReplyError("I could not find any user called `%s`!");

            var s = DatabaseUtil.getSessionFactory().openSession();
            var cb = s.getCriteriaBuilder();

            var q = cb.createQuery(Username.class);
            var r = q.from(Username.class);

            var names = s.createQuery(q.select(r).where(
                cb.equal(r.get("userId"), userRef)
            )).getResultList();

            var paged = new PagedEmbed(
                EmbedUtil.pagedDescription(
                    template,
                    names.isEmpty() ?
                        List.of("n/a") :
                        names.stream().sorted(Comparator.comparingLong(Username::getTimestamp).reversed())
                            .map(n -> n.getName() + "\n").collect(Collectors.toList())),
                event
            );

            PageListener.add(paged);
        }
    }
}
