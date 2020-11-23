package me.zoemartin.rubie.modules.embeds;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.annotations.LoadModule;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import me.zoemartin.rubie.modules.embeds.pinnedEmbeds.*;
import me.zoemartin.rubie.modules.embeds.triggerEmbeds.*;

@LoadModule
public class EmbedModule implements Module {
    @Override
    public void init() {
        Bot.addListener(new TeeController());
    }

    @Override
    public void initLate() {
        PineController.init();
        TeeController.init();
    }
}
