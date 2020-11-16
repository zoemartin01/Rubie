package me.zoemartin.rubie.core.managers;

import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.interfaces.Module;
import org.reflections8.Reflections;
import org.reflections8.scanners.SubTypesScanner;
import org.reflections8.scanners.TypeAnnotationsScanner;
import org.reflections8.util.ClasspathHelper;
import org.reflections8.util.ConfigurationBuilder;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleManager {
    private static final Collection<Module> modules = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void init() {
        /*Reflections reflections = new Reflections(new ConfigurationBuilder()
                                                      .setUrls(ClasspathHelper.forPackage("me.zoemartin.rubie.modules"))
                                                      .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
                                                      .setExecutorService(Executors.newFixedThreadPool(4)));*/
        // This is a really dumb way to suppress all the warns coming from java reflections
        // but it works so ¯\_(ツ)_/¯
        PrintStream err = System.err;
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        Reflections reflections = new Reflections(ClasspathHelper.forJavaClassPath(), new TypeAnnotationsScanner(), new SubTypesScanner());
        System.setErr(err);


        Set<Class<?>> modules = reflections.getTypesAnnotatedWith(LoadModule.class);
        modules.removeIf(aClass -> !Set.of(aClass.getInterfaces()).contains(Module.class));
        modules.removeIf(aClass -> {
            try {
                aClass.getMethod("init");
                return false;
            } catch (NoSuchMethodException e) {
                return true;
            }
        });

        modules.forEach(module -> {
            new Thread(() -> loadModule(module)).start();
        });
    }

    private static void loadModule(Class<?> module) {
        Module m = null;
        try {
            m = (Module) Stream.of(module.getConstructors()).findAny()
                             .orElseThrow(RuntimeException::new).newInstance();
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        if (m == null) return;
        m.init();
        loadModuleCommands(m);
        ModuleManager.modules.add(m);
        System.out.printf("Loaded Module '%s'\n", module.getName());
    }

    @SuppressWarnings("unchecked")
    private static void loadModuleCommands(Module m) {
        Reflections reflections = new Reflections(m.getClass().getPackageName(), new TypeAnnotationsScanner(), new SubTypesScanner());

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

                command.subCommands();
                CommandManager.register(command);
                System.out.printf("Loaded Module '%s' command '%s'\n", m.getClass().getName(), command.name());
            });

    }

    public static void initLate() {
        modules.forEach(Module::initLate);
    }
}
