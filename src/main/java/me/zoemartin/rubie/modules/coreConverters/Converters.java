package me.zoemartin.rubie.modules.coreConverters;

import me.zoemartin.rubie.core.AutoConfig;
import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.interfaces.ModuleInterface;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.entities.TextChannel;

@Module
public class Converters implements ModuleInterface {
    @Override
    public void init() {
        AutoConfig.registerConverter(
            Boolean.class,
            (event, s) -> switch (s.toLowerCase()) {
                case "false", "0", "yes" -> false;
                case "true", "1", "no" -> true;
                default -> throw new IllegalArgumentException();
            }
        );

        AutoConfig.registerConverter(
            Long.class, (event, s) -> {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException();
                }
            });

        AutoConfig.registerConverter(TextChannel.class, (event, s) -> Parser.Channel.getTextChannel(event.getGuild(), s));
    }
}
