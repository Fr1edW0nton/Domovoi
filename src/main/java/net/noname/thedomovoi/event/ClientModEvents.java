package net.noname.thedomovoi.event;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.noname.thedomovoi.TheDomovoi;
import net.noname.thedomovoi.entity.ModEntities;
import net.noname.thedomovoi.entity.bug.BugModel;
import net.noname.thedomovoi.entity.bug.BugRenderer;
import net.noname.thedomovoi.entity.domovoi.render.DomovoiModel;
import net.noname.thedomovoi.entity.domovoi.render.DomovoiRenderer;
import net.noname.thedomovoi.entity.domovoi.render.render_models.*;
import net.noname.thedomovoi.entity.moth.MothModel;
import net.noname.thedomovoi.entity.moth.MothRenderer;

@EventBusSubscriber(
        modid = TheDomovoi.MOD_ID,
        value = Dist.CLIENT
)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerEntityRenderers( EntityRenderersEvent.RegisterRenderers event ) {
        event.registerEntityRenderer(
                ModEntities.MOTH.get(),
                context -> new MothRenderer<>( context, new MothModel(
                        Identifier.fromNamespaceAndPath(
                                TheDomovoi.MOD_ID,
                                "moth"
                        )
                ) )
        );

        event.registerEntityRenderer(
                ModEntities.BUG.get(),
                context -> new BugRenderer<>( context, new BugModel(
                        Identifier.fromNamespaceAndPath(
                                TheDomovoi.MOD_ID,
                                "bug"
                        )
                ) )
        );

        event.registerEntityRenderer(
                ModEntities.DOMOVOI.get(),
                context -> new DomovoiRenderer( context, new DomovoiModel(
                        Identifier.fromNamespaceAndPath(
                                TheDomovoi.MOD_ID,
                                "domovoi"
                        )
                ) )
        );
    }

    @SubscribeEvent
    public static void registerLayerDefinitions( EntityRenderersEvent.RegisterLayerDefinitions event ) {
        event.registerLayerDefinition(
                HeldModelLayerLocations.MOTH_HELD_ITEM,
                HeldMothModel::createBodyLayer
        );

        event.registerLayerDefinition(
                HeldModelLayerLocations.BUG_HELD_ITEM,
                HeldBugModel::createBodyLayer
        );

        event.registerLayerDefinition(
                HeldModelLayerLocations.MILK_HELD_ITEM,
                HeldMilkModel::createBodyLayer
        );

        event.registerLayerDefinition(
                HeldModelLayerLocations.BREAD_HELD_ITEM,
                HeldBreadModel::createBodyLayer
        );
    }
}
