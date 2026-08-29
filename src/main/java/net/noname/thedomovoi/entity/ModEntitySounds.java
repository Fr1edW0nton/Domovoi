package net.noname.thedomovoi.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.noname.thedomovoi.TheDomovoi;

public class ModEntitySounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS
            = DeferredRegister.create( BuiltInRegistries.SOUND_EVENT, TheDomovoi.MOD_ID );


    public static final Holder<SoundEvent> MOTH_SOUND = SOUND_EVENTS.register(
            "moth_sound",
            SoundEvent::createVariableRangeEvent
    );

    public static final Holder<SoundEvent> BUG_SOUND = SOUND_EVENTS.register(
            "bug_sound",
            SoundEvent::createVariableRangeEvent
    );

    public static final Holder<SoundEvent> DOMOVOI_SWEEP = SOUND_EVENTS.register(
            "domovoi_sweep",
            SoundEvent::createVariableRangeEvent
    );

    public static final Holder<SoundEvent> DOMOVOI_DUST = SOUND_EVENTS.register(
            "domovoi_dust",
            SoundEvent::createVariableRangeEvent
    );



    public static void register( IEventBus eventBus ) { SOUND_EVENTS.register( eventBus ); }
}
