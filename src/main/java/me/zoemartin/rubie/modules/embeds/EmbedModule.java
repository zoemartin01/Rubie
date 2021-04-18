package me.zoemartin.rubie.modules.embeds;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.annotations.Module;
import me.zoemartin.rubie.core.interfaces.ModuleInterface;
import me.zoemartin.rubie.modules.embeds.pinnedEmbeds.PineController;
import me.zoemartin.rubie.modules.embeds.triggerEmbeds.TeeController;

@Module
public class EmbedModule implements ModuleInterface {
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
