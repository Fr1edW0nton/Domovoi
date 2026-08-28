package net.noname.thedomovoi.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.noname.thedomovoi.TheDomovoi;
import net.noname.thedomovoi.entity.bug.Bug;
import net.noname.thedomovoi.entity.domovoi.Domovoi;
import net.noname.thedomovoi.entity.moth.Moth;

import java.util.function.Supplier;

public class ModEntities {

    public static final DeferredRegister.Entities ENTITY_TYPES
            = DeferredRegister.createEntities( TheDomovoi.MOD_ID );


    public static final Supplier<EntityType<Domovoi>> DOMOVOI = ENTITY_TYPES.registerEntityType(
            "domovoi",
            Domovoi::new,
            MobCategory.MISC
    );

    public static final Supplier<EntityType<Moth>> MOTH = ENTITY_TYPES.registerEntityType(
            "moth",
            Moth::new,
            MobCategory.MISC
    );

    public static final Supplier<EntityType<Bug>> BUG = ENTITY_TYPES.registerEntityType(
            "bug",
            Bug::new,
            MobCategory.MISC
    );



    public static void register( IEventBus eventBus ) { ENTITY_TYPES.register( eventBus ); }
}
