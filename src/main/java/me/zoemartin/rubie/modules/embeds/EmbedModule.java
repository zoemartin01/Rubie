package me.zoemartin.rubie.modules.embeds;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import me.zoemartin.rubie.modules.embeds.pinnedEmbeds.*;
import me.zoemartin.rubie.modules.embeds.triggerEmbeds.*;

@LoadModule
public class EmbedModule implements Module {
    @Override
    public void init() {
        DatabaseUtil.setMapped(PineEntity.class);
        DatabaseUtil.setMapped(Tee.class);
        Bot.addListener(new TeeController());
        CommandManager.register(new EmbedSource());
        CommandManager.register(new CustomEmbed());
        CommandManager.register(new PineCommand());
        CommandManager.register(new TeeCommand());
    }

    @Override
    public void initLate() {
        PineController.init();
        TeeController.init();
    }
}
