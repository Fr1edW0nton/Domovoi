package net.noname.thedomovoi.entity.domovoi.render.render_layers;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.noname.thedomovoi.TheDomovoi;
import net.noname.thedomovoi.entity.domovoi.Domovoi;
import net.noname.thedomovoi.entity.domovoi.render.DomovoiRenderState;
import net.noname.thedomovoi.entity.domovoi.render.DomovoiRenderer;
import net.noname.thedomovoi.entity.domovoi.render.render_models.HeldBreadModel;
import net.noname.thedomovoi.entity.domovoi.render.render_models.HeldBugModel;
import net.noname.thedomovoi.entity.domovoi.render.render_models.HeldMilkModel;
import net.noname.thedomovoi.entity.domovoi.render.render_models.HeldMothModel;
import org.jspecify.annotations.NonNull;

import java.util.function.BiConsumer;

public class DomovoiItemInHandLayer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoRenderLayer<Domovoi, Void, @NonNull R> {

    private static final String HAND_BONE = "hand";

    private final HeldMothModel mothModel;
    private final HeldBugModel bugModel;
    private final HeldMilkModel milkModel;
    private final HeldBreadModel breadModel;

    private static final Identifier MOTH_BROWN = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/hand_held/moth/held_moth_brown.png"
    );
    private static final Identifier MOTH_GRAY = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/hand_held/moth/held_moth_gray.png"
    );
    private static final Identifier MOTH_WHITE = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/hand_held/moth/held_moth_white.png"
    );

    private static final Identifier BUG_BROWN = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/hand_held/bug/held_bug_brown.png"
    );
    private static final Identifier BUG_GRAY = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/hand_held/bug/held_bug_gray.png"
    );

    private static final Identifier MILK = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/hand_held/milk/held_milk.png"
    );

    private static final Identifier BREAD = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/hand_held/bread/held_bread.png"
    );

    public DomovoiItemInHandLayer(
            GeoRenderer<Domovoi, Void, @NonNull R> renderer,
            HeldMothModel mothModel,
            HeldBugModel bugModel,
            HeldMilkModel milkModel,
            HeldBreadModel breadModel
    ) {
        super(renderer);

        this.mothModel  = mothModel;
        this.bugModel   = bugModel;
        this.milkModel  = milkModel;
        this.breadModel = breadModel;
    }



    private void renderMoth(
            RenderPassInfo<@NonNull R> pRenderState,
            GeoBone pBone,
            SubmitNodeCollector pRenderTasks
    ) {
        int consumeMobVariant = pRenderState.getOrDefaultGeckolibData(
                DomovoiRenderer.CONSUME_MOB_VARIANT,
                0
        );

        Identifier mothTexture;
        switch ( consumeMobVariant ) {
            case 1 -> mothTexture = MOTH_GRAY;
            case 2 -> mothTexture = MOTH_WHITE;
            default -> mothTexture = MOTH_BROWN;
        }

        PoseStack poseStack = pRenderState.poseStack();

        poseStack.pushPose();

        poseStack.translate( 0.1F, -1.7F, 0.0F );
        poseStack.scale( 1.0F, 1.0F, 1.0F );

        RenderType renderType = RenderTypes.entityCutoutCull( mothTexture );

        pRenderTasks.submitModel(
                this.mothModel,
                pRenderState.renderState(),
                poseStack,
                renderType,
                pRenderState.packedLight(),
                OverlayTexture.NO_OVERLAY,
                0,
                null
        );

        poseStack.popPose();
    }

    private void renderBug(
            RenderPassInfo<@NonNull R> pRenderState,
            GeoBone pBone,
            SubmitNodeCollector pRenderTasks
    ) {
        int consumeMobVariant = pRenderState.getOrDefaultGeckolibData(
                DomovoiRenderer.CONSUME_MOB_VARIANT,
                0
        );

        Identifier bugTexture;
        if ( consumeMobVariant == 1 ) {
            bugTexture = BUG_GRAY;
        } else {
            bugTexture = BUG_BROWN;
        }

        PoseStack poseStack = pRenderState.poseStack();

        poseStack.pushPose();

        poseStack.translate( 0.1F, -1.70F, 0.0F );
        poseStack.scale( 1.0F, 1.0F, 1.0F );

        RenderType renderType = RenderTypes.entityCutoutCull( bugTexture );

        pRenderTasks.submitModel(
                this.bugModel,
                pRenderState.renderState(),
                poseStack,
                renderType,
                pRenderState.packedLight(),
                OverlayTexture.NO_OVERLAY,
                0,
                null
        );

        poseStack.popPose();
    }

    private void renderMilk(
            RenderPassInfo<@NonNull R> pRenderState,
            GeoBone pBone,
            SubmitNodeCollector pRenderTasks
    ) {
        PoseStack poseStack = pRenderState.poseStack();

        poseStack.pushPose();

        poseStack.translate( -0.15F, -1.75F, 0.0F );
        poseStack.scale( 1.0F, 1.0F, 1.0F );

        RenderType renderType = RenderTypes.entityCutoutCull( MILK );

        pRenderTasks.submitModel(
                this.milkModel,
                pRenderState.renderState(),
                poseStack,
                renderType,
                pRenderState.packedLight(),
                OverlayTexture.NO_OVERLAY,
                0,
                null
        );

        poseStack.popPose();
    }

    private void renderBread(
            RenderPassInfo<@NonNull R> pRenderState,
            GeoBone pBone,
            SubmitNodeCollector pRenderTasks
    ) {
        PoseStack poseStack = pRenderState.poseStack();

        poseStack.pushPose();

        poseStack.translate( -0.15F, -1.75F, 0.0F );
        poseStack.scale( 1.0F, 1.0F, 1.0F );

        RenderType renderType = RenderTypes.entityCutoutCull( BREAD );

        pRenderTasks.submitModel(
                this.breadModel,
                pRenderState.renderState(),
                poseStack,
                renderType,
                pRenderState.packedLight(),
                OverlayTexture.NO_OVERLAY,
                0,
                null
        );

        poseStack.popPose();
    }



    private void renderHeldItem(
            RenderPassInfo<@NonNull R> pRenderState,
            GeoBone pBone,
            SubmitNodeCollector pRenderTasks
    ) {
        Domovoi.ConsumeType consumeType = pRenderState.getOrDefaultGeckolibData(
                DomovoiRenderer.CONSUME_TYPE,
                Domovoi.ConsumeType.NONE
        );
        if ( consumeType == Domovoi.ConsumeType.NONE ) { return; }

        switch ( consumeType ) {
            case MOTH -> this.renderMoth( pRenderState, pBone, pRenderTasks );
            case BUG -> this.renderBug( pRenderState, pBone, pRenderTasks );
            case MILK -> this.renderMilk( pRenderState, pBone, pRenderTasks );
            case BREAD -> this.renderBread( pRenderState, pBone, pRenderTasks );
        }
    }



    @Override
    public void addPerBoneRender(
            @NonNull RenderPassInfo<@NonNull R> renderPassInfo,
            @NonNull BiConsumer<GeoBone, PerBoneRender<@NonNull R>> consumer
    ) {
        renderPassInfo.model().getBone( HAND_BONE ).ifPresent( hand -> {

            consumer.accept(
                    hand,
                    this::renderHeldItem
            );
        } );
    }
}
