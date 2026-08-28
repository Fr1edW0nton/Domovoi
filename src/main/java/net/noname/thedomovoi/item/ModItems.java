package net.noname.thedomovoi.item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.noname.thedomovoi.TheDomovoi;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems( TheDomovoi.MOD_ID );


    public static void register( IEventBus eventBus ) { ITEMS.register( eventBus ); }
}
