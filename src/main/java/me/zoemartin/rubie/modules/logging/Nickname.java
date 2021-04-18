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
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@DatabaseEntity
@Entity
@Table(name = "nicknames")
public class Nickname implements DatabaseEntry {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID key;

    @Column(name = "guild_id")
    private String guildId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "name")
    private String name;

    @Column(name = "timestamp")
    private long timestamp;

    public Nickname() {
    }

    public Nickname(Member member) {
        this.guildId = member.getGuild().getId();
        this.userId = member.getId();
        this.name = member.getNickname();
        this.timestamp = Instant.now().toEpochMilli();
    }

    public Nickname(Member member, String nick) {
        this.guildId = member.getGuild().getId();
        this.userId = member.getId();
        this.name = nick;
        this.timestamp = Instant.now().toEpochMilli() - 1;
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    static class NicknameListener extends ListenerAdapter {
        @Override
        public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
            var s = DatabaseUtil.getSessionFactory().openSession();
            var cb = s.getCriteriaBuilder();

            var q = cb.createQuery(Nickname.class);
            var r = q.from(Nickname.class);
            var l = s.createQuery(q.select(r).where(
                cb.equal(r.get("guildId"), event.getGuild().getId()),
                cb.equal(r.get("userId"), event.getUser().getId()),
                cb.equal(r.get("name"), event.getOldNickname())
            )).getResultList();

            if (l.isEmpty() && event.getOldNickname() != null)
                DatabaseUtil.saveObject(new Nickname(event.getMember(), event.getOldNickname()));
            if (event.getNewNickname() != null) DatabaseUtil.saveObject(new Nickname(event.getMember()));
        }
    }

    @Command
    @CommandOptions(
        name = "nicknames",
        description = "Shows a users past nicknames",
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

                if (user == null) template = new EmbedBuilder().setTitle("Nicknames for `" + userRef + "`").build();
                else template = new EmbedBuilder()
                                    .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                                    .setTitle("Nicknames")
                                    .build();

            } else if (Parser.User.tagIsParsable(ref)) {
                var user = event.getJDA().getUserByTag(ref);
                Check.entityReferenceNotNull(user, User.class, ref);

                userRef = user.getId();
                template = new EmbedBuilder()
                               .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                               .setTitle("Nicknames")
                               .build();
            } else throw new ReplyError("I could not find any user called `%s`!");

            var s = DatabaseUtil.getSessionFactory().openSession();
            var cb = s.getCriteriaBuilder();

            var q = cb.createQuery(Nickname.class);
            var r = q.from(Nickname.class);

            var nicks = s.createQuery(q.select(r).where(
                cb.equal(r.get("guildId"), event.getGuild().getId()),
                cb.equal(r.get("userId"), userRef)
            )).getResultList();

            var paged = new PagedEmbed(
                EmbedUtil.pagedDescription(
                    template,
                    nicks.isEmpty() ?
                        List.of("n/a") :
                        nicks.stream().sorted(Comparator.comparingLong(Nickname::getTimestamp).reversed()).map(n -> n.getName() + "\n").collect(Collectors.toList())),
                event
            );

            PageListener.add(paged);
        }
    }
}
