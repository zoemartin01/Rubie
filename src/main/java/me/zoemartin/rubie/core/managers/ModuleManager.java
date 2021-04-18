package me.zoemartin.rubie.core.managers;

import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.interfaces.*;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import org.reflections8.Reflections;
import org.reflections8.scanners.SubTypesScanner;
import org.reflections8.scanners.TypeAnnotationsScanner;
import org.reflections8.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ModuleManager {
    private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);
    private static final Collection<ModuleInterface> modules = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void init() {
        loadDatabaseMappings();

        ExecutorService es = Executors.newCachedThreadPool();
        loadModules().parallelStream().forEach(module -> es.execute(() -> initModules(module)));

        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Collection<ModuleInterface> getModules() {
        return Collections.unmodifiableCollection(modules);
    }

    private static Collection<ModuleInterface> loadModules() {
        var reflections = new Reflections(new ConfigurationBuilder()
                                              .setUrls(ClasspathHelper.forPackage("me.zoemartin.rubie.modules"))
                                              .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                                              .filterInputsBy(new FilterBuilder().includePackage("me.zoemartin.rubie.modules"))
                                              .setExecutorService(Executors.newFixedThreadPool(4)));

        var classes = reflections.getTypesAnnotatedWith(Module.class);

        return classes.stream()
                   .filter(c -> c.getAnnotationsByType(Disabled.class).length == 0)
                   .map(aClass -> {
                       try {
                           if (Arrays.stream(aClass.getInterfaces()).noneMatch(clazz -> clazz == ModuleInterface.class)) {
                               log.error("Trying to load a class {} that doesn't implement the module interface", aClass.getSimpleName());
                               return null;
                           }
                           var constructor = aClass.getDeclaredConstructor();
                           return (ModuleInterface) constructor.newInstance();
                       } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                           log.error("Error getting default constructor for module", e);
                           return null;
                       }
                   })
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    private static void initModules(ModuleInterface m) {
        m.init();
        loadModuleCommands(m);
        ModuleManager.modules.add(m);
        log.info("Loaded '{}'", m.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static void loadModuleCommands(ModuleInterface m) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                                                      .setUrls(ClasspathHelper.forPackage(m.getClass().getPackageName()))
                                                      .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                                                      .filterInputsBy(new FilterBuilder().includePackage(m.getClass().getPackageName()))
                                                      .setExecutorService(Executors.newFixedThreadPool(4)));

        Set<Class<?>> commandReflect = reflections.getTypesAnnotatedWith(Command.class);

        commandReflect.stream()
            .filter(c -> c.getAnnotationsByType(Disabled.class).length == 0)
            .forEach(c -> {
                AbstractCommand command = null;
                try {
                    Constructor<? extends AbstractCommand> constructor =
                        (Constructor<? extends AbstractCommand>)
                            Arrays.stream(c.getDeclaredConstructors()).findAny().orElseThrow(
                                () -> new IllegalStateException("Command Class missing a public no-args Constructor"));
                    constructor.setAccessible(true);
                    command = constructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    System.err.println(e.getMessage());
                }
                if (command == null) return;

                sub(command);
                CommandManager.register(command);
                log.info("Loaded '{}' command '{}'", m.getClass().getName(), command.name());
            });
    }

    private static void loadDatabaseMappings() {
        var reflections = new Reflections(new ConfigurationBuilder()
                                              .setUrls(ClasspathHelper.forPackage("me.zoemartin.rubie"))
                                              .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                                              .filterInputsBy(new FilterBuilder().includePackage("me.zoemartin.rubie"))
                                              .setExecutorService(Executors.newFixedThreadPool(4)));

        var dbm = reflections.getTypesAnnotatedWith(DatabaseEntity.class);

        dbm.stream()
            .filter(c -> c.getAnnotationsByType(Disabled.class).length == 0)
            .filter(aClass -> {
                if (Arrays.stream(aClass.getInterfaces()).anyMatch(clazz -> clazz == DatabaseEntry.class))
                    return true;
                log.error("Trying to load a class {} that doesn't implement the database entry interface", aClass.getSimpleName());
                return false;
            })
            .forEach(c -> {
                DatabaseUtil.setMapped(c);
                log.info("Mapped '{}':'{}'", c.getPackageName(), c.getSimpleName());
            });
    }


    private static void sub(AbstractCommand c) {
        if (c.getClass().getAnnotationsByType(SubCommand.AsBase.class).length != 0)
            CommandManager.register(subcommand(c));


        if (!c.subCommands().isEmpty()) {
            c.subCommands().forEach(ModuleManager::sub);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractCommand> T subcommand(T c) {
        SubCommand.AsBase asBase = c.getClass().getAnnotationsByType(SubCommand.AsBase.class)[0];

        CommandConfiguration conf = new CommandConfiguration(c.subCommands(), asBase.name(), c.commandPerm(), c.usage(), c.description(), c.getConfiguration().getBotPerms(), asBase.alias(), c.getConfiguration().getHelp(),
            c.getConfiguration().isHidden());

        if (c instanceof GuildCommand) {
            return (T) new GuildCommand(conf) {
                @Override
                public void run(GuildCommandEvent event) {
                    ((GuildCommand) c).run(event);
                }
            };
        } else {
            return (T) new AbstractCommand(conf) {
                @Override
                public void run(CommandEvent event) {
                    c.run(event);
                }
            };
        }

    }

    public static void initLate() {
        modules.forEach(ModuleInterface::initLate);
    }
}
