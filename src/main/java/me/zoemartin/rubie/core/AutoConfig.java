package me.zoemartin.rubie.core;

import me.zoemartin.rubie.core.annotations.Getter;
import me.zoemartin.rubie.core.annotations.Setter;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.exceptions.UnexpectedError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.DatabaseUtil;

import javax.persistence.Entity;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AutoConfig<T> extends GuildCommand {
    private static final Map<Class<?>, BiFunction<GuildCommandEvent, String, ?>> converters = new ConcurrentHashMap<>();

    public static <T> void registerConverter(Class<T> clazz, BiFunction<GuildCommandEvent, String, T> converter) {
        converters.put(clazz, converter);
    }

    // good idea, bad impl. has to do so much stuff every time autoconfig is run even to the things it reads don't change on runtime
    // TODO: fix that
    @Override
    public void run(GuildCommandEvent event) {
        T t = supply(event);

        var getter = getGetter(t.getClass());
        var setter = getSetter(t.getClass());
        var args = event.getArgs();

        if (args.isEmpty()) {
            var keys = setter.entrySet().stream()
                           .filter(e -> getter.containsKey(e.getKey()))
                           .map(e ->
                                    String.format("`%s` - Type: `%s`", e.getKey(),
                                        e.getValue().getAnnotation(Setter.class).type().isBlank() ?
                                            e.getValue().getParameterTypes()[0].getSimpleName() :
                                            e.getValue().getAnnotation(Setter.class).type()))
                           .collect(Collectors.joining("\n"));

            event.reply("Valid Configuration Keys", String.join("\n", keys)).queue();
        } else if (args.size() == 1) {
            var key = args.get(0);
            var get = getter.getOrDefault(key, null);
            Check.notNull(get, () -> new ReplyError("'%s' not a valid configuration key!", key));

            try {
                event.reply("Key: `" + key + "`", "Value: `%s`", get.invoke(t)).queue();
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnexpectedError();
            }
        } else {
            var key = args.get(0);
            var value = lastArg(1, event);
            var set = setter.get(key);
            Check.notNull(set, () -> new ReplyError("'%s' not a valid configuration key!", key));

            var converter = converters.getOrDefault(set.getParameterTypes()[0], null);
            Check.notNull(converter, () -> new UnexpectedError("No Value Converter available for %s!",
                set.getParameterTypes()[0].getSimpleName()));

            try {
                var converted = converter.apply(event, value);
                set.invoke(t, converted);

                if (t.getClass().getAnnotationsByType(Entity.class).length != 0) DatabaseUtil.updateObject(t);

                event.reply(null, "`%s` set to `%s`", key, converted).queue();
                event.addCheckmark();
            } catch (IllegalArgumentException e) {
                throw new ReplyError("`%s` is not a valid argument for `%s`", value, key);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnexpectedError();
            }
        }

    }

    protected abstract T supply(GuildCommandEvent event);

    private static Map<String, Method> getSetter(final Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                   .filter(method -> method.isAnnotationPresent(Setter.class))
                   .collect(Collectors.toConcurrentMap(method -> {
                           String name = method.getAnnotation(Setter.class).name();
                           if (name.isBlank()) name = method.getName();
                           return name;
                       },
                       Function.identity()));
    }

    private static Map<String, Method> getGetter(final Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                   .filter(method -> method.isAnnotationPresent(Getter.class))
                   .collect(Collectors.toConcurrentMap(method -> {
                           String name = method.getAnnotation(Getter.class).value();
                           if (name.isBlank()) name = method.getName();
                           return name;
                       },
                       Function.identity()));
    }
}
