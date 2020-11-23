package me.zoemartin.rubie.modules.commandProcessing;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.util.CollectorsUtil;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PermissionHandler {
    private static final Map<String, Collection<MemberPermission>> memberPerms = new ConcurrentHashMap<>();
    private static final Map<String, Collection<RolePermission>> rolePerms = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(PermissionHandler.class);

    public static void initPerms() {
        memberPerms.putAll(DatabaseUtil.loadGroupedCollection(
            "from MemberPermission", MemberPermission.class,
            MemberPermission::getGuild_id,
            Function.identity(),
            CollectorsUtil.toConcurrentSet()
        ));

        memberPerms.forEach((s, e) -> log.info("Loaded '{}' member perm overrides for '{}'", e.size(), s));

        rolePerms.putAll(DatabaseUtil.loadGroupedCollection(
            "from RolePermission", RolePermission.class,
            RolePermission::getGuild_id,
            Function.identity(),
            CollectorsUtil.toConcurrentSet()
        ));

        rolePerms.forEach((s, e) -> log.info("Loaded '{}' role perm overrides for '{}'", e.size(), s));
    }

    public static boolean checkUserPerms(AbstractCommand command, CommandEvent event) {
        if (command.commandPerm() == CommandPerm.EVERYONE) return true;
        if (event.getUser().getId().equals(Bot.getOWNER())) return true;

        if (event instanceof GuildCommandEvent) {
            return getHighestFromUser(((GuildCommandEvent) event).getGuild(), ((GuildCommandEvent) event).getMember()).raw() >= command.commandPerm().raw();
        }

        return false;
    }

    public static MemberPermission getMemberPerm(String guildId, String memberId) {
        return memberPerms.getOrDefault(guildId, Collections.emptySet())
                   .stream().filter(memberPermission -> memberPermission.getMember_id().equals(memberId))
                   .findAny().orElse(new MemberPermission(guildId, memberId, CommandPerm.EVERYONE));
    }

    public static RolePermission getRolePerm(String guildId, String roleId) {
        return rolePerms.getOrDefault(guildId, Collections.emptySet())
                   .stream().filter(memberPermission -> memberPermission.getRole_id().equals(roleId))
                   .findAny().orElse(new RolePermission(guildId, roleId, CommandPerm.EVERYONE));
    }

    public static CommandPerm getHighestFromUser(Guild g, Member m) {
        int perm = Integer.max(getMemberPerm(g.getId(), m.getId()).getPerm().raw(),
            m.getRoles().stream()
                .map(role -> getRolePerm(g.getId(), role.getId()).getPerm())
                .max(Comparator.comparingInt(CommandPerm::raw)).orElse(CommandPerm.EVERYONE).raw());

        if (m.hasPermission(Permission.ADMINISTRATOR))
            return CommandPerm.fromNum(Integer.max(perm, CommandPerm.BOT_ADMIN.raw()));
        else return CommandPerm.fromNum(perm);
    }

    public static void setRolePerm(String guildId, String roleId, CommandPerm perm) {
        RolePermission rp;
        if (!getMemberPerm(guildId, roleId).getPerm().equals(CommandPerm.EVERYONE)) {
            rp = getRolePerm(guildId, roleId);
            rp.setPerm(perm);
            DatabaseUtil.updateObject(rp);
        } else {
            rp = new RolePermission(guildId, roleId, perm);
            DatabaseUtil.saveObject(rp);
        }
        rolePerms.computeIfAbsent(guildId, s -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
            .add(rp);
    }

    public static void addMemberPerm(String guildId, String memberId, CommandPerm perm) {
        MemberPermission mp;
        if (!getMemberPerm(guildId, memberId).getPerm().equals(CommandPerm.EVERYONE)) {
            mp = getMemberPerm(guildId, memberId);
            mp.setPerm(perm);
            DatabaseUtil.updateObject(mp);
        } else {
            mp = new MemberPermission(guildId, memberId, perm);
            DatabaseUtil.saveObject(mp);
        }
        memberPerms.computeIfAbsent(guildId, s -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
            .add(mp);
    }

    public static boolean removeMemberPerm(String guildId, String memberId) {
        MemberPermission mp = memberPerms.getOrDefault(guildId,
            Collections.emptySet()).stream().filter(memberPermission -> memberPermission.getMember_id().equals(memberId))
                                  .findFirst().orElse(null);

        if (mp == null) return false;

        DatabaseUtil.deleteObject(mp);
        return memberPerms.get(guildId).remove(mp);
    }

    public static boolean removeRolePerm(String guildId, String roleId) {
        RolePermission rp = rolePerms.getOrDefault(guildId,
            Collections.emptySet()).stream().filter(memberPermission -> memberPermission.getRole_id().equals(roleId))
                                .findFirst().orElse(null);

        if (rp == null) return false;

        DatabaseUtil.deleteObject(rp);
        return rolePerms.get(guildId).remove(rp);
    }

    public static Collection<MemberPermission> getMemberPerms(String guildId) {
        return memberPerms.getOrDefault(guildId, Collections.emptySet());
    }

    public static Collection<RolePermission> getRolePerms(String guildId) {
        return rolePerms.getOrDefault(guildId, Collections.emptySet());
    }
}
