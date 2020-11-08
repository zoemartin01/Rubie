package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ConsoleError;
import me.zoemartin.rubie.core.interfaces.*;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@LoadModule
public class Debug implements Module {
    @Override
    public void init() {
        CommandManager.register(new Shutdown());
        CommandManager.register(new Purge());
        CommandManager.register(new Dump());
        CommandManager.register(new ReadError());
        CommandManager.register(new Transcript());
        CommandManager.register(new Output());
        CommandManager.register(new IDTime());
    }
}
