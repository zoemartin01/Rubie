package me.zoemartin.rubie.modules.hatchery;

import me.zoemartin.rubie.core.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import net.dv8tion.jda.api.entities.Guild;

@LoadModule
public class Hatchery implements Module {
    private static final String GUILD_ID = "531790991586361344";

    @Override
    public void init() {
        CommandManager.register(new MVC());
    }


    public static boolean isHatchery(Guild g) {
        return g.getId().equals(GUILD_ID);
    }
}
