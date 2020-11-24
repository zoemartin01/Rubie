package me.zoemartin.rubie.modules.embeds;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.modules.embeds.pinnedEmbeds.PineController;
import me.zoemartin.rubie.modules.embeds.triggerEmbeds.TeeController;

@AutoService(Module.class)
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
