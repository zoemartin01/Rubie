package me.zoemartin.rubie.core;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.EnumSet;

@Converter
public enum CommandPerm {
    EVERYONE(0, "EVERYONE"),
    BOT_USER(1, "USER"),
    BOT_MODERATOR(2, "MODERATOR"),
    BOT_MANAGER(3, "MANAGER"),
    BOT_ADMIN(4, "ADMIN"),
    OWNER(5, "OWNER");

    private final int num;
    private final String name;

    CommandPerm(int num, String name) {
        this.num = num;
        this.name = name;
    }

    public int raw() {
        return num;
    }

    public static CommandPerm fromNum(Integer num) {
        if (num == null) return null;

        return EnumSet.allOf(CommandPerm.class).stream().filter(perm -> num.equals(perm.raw())).findAny()
                   .orElse(null);
    }

    public static CommandPerm fromString(String name) {
        if (name == null) return null;

        return EnumSet.allOf(CommandPerm.class).stream().filter(perm -> name.equalsIgnoreCase(perm.name)
                                                                            || name.equalsIgnoreCase(perm.name())).findAny()
                   .orElse(null);
    }

    @Override
    public String toString() {
        return name;
    }

    public static class Converter implements AttributeConverter<CommandPerm, Integer> {
        @Override
        public Integer convertToDatabaseColumn(CommandPerm attribute) {
            return attribute.raw();
        }

        @Override
        public CommandPerm convertToEntityAttribute(Integer dbData) {
            return CommandPerm.fromNum(dbData);
        }
    }
}
