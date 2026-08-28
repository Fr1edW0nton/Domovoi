package net.noname.thedomovoi.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.noname.thedomovoi.TheDomovoi;
import net.noname.thedomovoi.entity.ModEntities;

@EventBusSubscriber( modid = TheDomovoi.MOD_ID )
public class ModEvents {

    @SubscribeEvent
    public static void createDefaultAttributes( EntityAttributeCreationEvent event ) {
        event.put(
                ModEntities.MOTH.get(),
                LivingEntity.createLivingAttributes()
                        .add( Attributes.MAX_HEALTH, 1.0F )
                        .add( Attributes.FLYING_SPEED, 0.2F )
                        .add( Attributes.FOLLOW_RANGE, 48.0F )
                        .build()
        );

        event.put(
                ModEntities.BUG.get(),
                LivingEntity.createLivingAttributes()
                        .add( Attributes.MAX_HEALTH, 1.0F )
                        .add( Attributes.MOVEMENT_SPEED, 0.2F )
                        .add( Attributes.FOLLOW_RANGE, 48.0F )
                        .build()
        );

        event.put(
                ModEntities.DOMOVOI.get(),
                LivingEntity.createLivingAttributes()
                        .add( Attributes.MAX_HEALTH, 1.0F )
                        .add( Attributes.MOVEMENT_SPEED, 0.25F )
                        .add( Attributes.FOLLOW_RANGE, 48.0F )
                        .build()
        );
    }
}
