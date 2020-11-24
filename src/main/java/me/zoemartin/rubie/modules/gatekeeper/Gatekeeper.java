package me.zoemartin.rubie.modules.gatekeeper;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.AutoConfig;
import me.zoemartin.rubie.core.annotations.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import me.zoemartin.rubie.modules.levels.LevelConfig;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@LoadModule
public class Gatekeeper extends ListenerAdapter implements Module {
    private static String URI;
    private static final Map<String, GatekeeperConfig> configs = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(Gatekeeper.class);

    @Override
    public void init() {
        if (!Boolean.parseBoolean(Bot.getProperties().getProperty("gatekeeper.websocket.enabled"))) return;
        WebSocket.startAPI();
        Bot.addListener(this);
        URI = Bot.getProperties().getProperty("gatekeeper.uri");

        AutoConfig.registerConverter(GatekeeperConfig.Mode.class,
            (event, s) -> EnumSet.allOf(GatekeeperConfig.Mode.class).stream()
                              .filter(mode -> s.toLowerCase().contains(mode.name().toLowerCase()))
                              .findAny().orElseThrow(IllegalArgumentException::new));
    }

    @Override
    public void initLate() {
        loadConfigs();
    }

    private static void loadConfigs() {
        configs.putAll(DatabaseUtil.loadMap("from GatekeeperConfig", GatekeeperConfig.class,
            GatekeeperConfig::getGuildId, Function.identity()));
        configs.forEach((s, config) -> log.info(
            "Loaded config for '{}'", config.getGuildId()));
        log.info("Loaded '{}' configuration files", configs.keySet().size());
    }

    static GatekeeperConfig getConfig(Guild g) {
        return configs.computeIfAbsent(g.getId(), v -> {
            GatekeeperConfig config = new GatekeeperConfig(g);
            DatabaseUtil.saveObject(config);
            return config;
        });
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        GatekeeperConfig config = getConfig(event.getGuild());

        if (!config.isGatekeeperEnabled() || config.getGatekeeperMode() != GatekeeperConfig.Mode.TRIGGER) return;

        if (event.getMessage().getContentRaw().equals(config.getVerificationTrigger())) {
            sendVerificationKey(event.getMember());

            if (config.isLoggingEnabled()) {
                TextChannel channel = event.getGuild().getTextChannelById(config.getLogChannelId());
                Member member = event.getMember();
                if (channel != null)
                    channel.sendMessageFormat(
                        "%s / %s / %s has passed the verification trigger and has been sent a verification link!",
                        Objects.requireNonNull(member).getEffectiveName(), member.getAsMention(), member.getId()).queue();
            }

            event.getMessage().addReaction("U+1F4EC").queue();
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        GatekeeperConfig config = getConfig(event.getGuild());

        if (!config.isGatekeeperEnabled()) return;

        if (config.getGatekeeperMode() != GatekeeperConfig.Mode.JOIN) return;

        sendVerificationKey(event.getMember());
    }

    private void sendVerificationKey(Member member) {
        VerificationEntity entity = new VerificationEntity(member);
        DatabaseUtil.saveObject(entity);

        member.getUser().openPrivateChannel().complete()
            .sendMessageFormat("Please verify on %s using the following link %s%s", member.getGuild().getName(),
                URI, entity.getKey()).queue();
    }

    static void verify(VerificationEntity entity) {
        if (!entity.isVerified()) return;

        Guild guild = Bot.getJDA().getGuildById(entity.getGuild_id());
        if (guild == null) return;
        Member member = guild.getMemberById(entity.getUser_id());
        if (member == null) return;

        GatekeeperConfig config = getConfig(guild);

        if (config.doSendMessage())
            member.getUser().openPrivateChannel().complete()
                .sendMessageFormat("You have successfully been verified on %s!", guild.getName()).queue();

        if (config.doAddRoles()) {
            config.getRolesToAdd().stream().map(guild::getRoleById).filter(Objects::nonNull)
                .forEach(role -> guild.addRoleToMember(member, role).queue());
        }

        if (config.doRemoveRoles())
            config.getRolesToRemove().stream().map(guild::getRoleById).filter(Objects::nonNull)
                .forEach(role -> guild.addRoleToMember(member, role).complete());

        if (config.isLoggingEnabled()) {
            TextChannel channel = guild.getTextChannelById(config.getLogChannelId());
            if (channel != null)
                channel.sendMessageFormat(
                    "%s / %s / %s has passed the gatekeeper and has been given access to the server!",
                    member.getEffectiveName(), member.getAsMention(), member.getId()).queue();
        }

        DatabaseUtil.deleteObject(entity);
    }
}
