package net.noname.thedomovoi.entity.domovoi.render.render_layers;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.noname.thedomovoi.entity.domovoi.Domovoi;
import net.noname.thedomovoi.entity.domovoi.render.DomovoiRenderState;
import net.noname.thedomovoi.entity.domovoi.render.DomovoiRenderer;
import org.jspecify.annotations.NonNull;

import java.util.function.BiConsumer;

public class DomovoiToolLayer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoRenderLayer<Domovoi, Void, @NonNull R> {

    private static final String BROOM_BONE = "broom";
    private static final String DUSTER_BONE = "duster";
    private static final String MOTH_BONE = "sleeping_moth";

    public DomovoiToolLayer( DomovoiRenderer renderer ) { super( renderer ); }



    private void renderTool(
            RenderPassInfo<@NonNull R> pRenderPassInfo,
            GeoBone pBone,
            SubmitNodeCollector pRenderTasks
    ) {
        if ( !( pBone instanceof CuboidGeoBone cuboidGeoBone ) ) { return; }

        RenderType renderType =
                RenderTypes.entityCutoutCull( getTextureResource( pRenderPassInfo.renderState() ) );

        pRenderTasks.submitCustomGeometry(
                pRenderPassInfo.poseStack(),
                renderType,
                ( pose, vertexConsumer ) -> {

                    PoseStack poseStack = pRenderPassInfo.poseStack();

                    poseStack.pushPose();

                    poseStack.last().set( pose );
                    pBone.translateAwayFromPivotPoint( poseStack );

                    for ( GeoCube geoCube : cuboidGeoBone.cubes ) {
                        poseStack.pushPose();

                        geoCube.render(
                                poseStack,
                                vertexConsumer,
                                pRenderPassInfo.packedLight(),
                                pRenderPassInfo.packedOverlay(),
                                0xFFFFFF
                        );

                        poseStack.popPose();
                    }

                    poseStack.popPose();
                }
        );
    }



    private void registerBoneTree(GeoBone pBone, BiConsumer<GeoBone, PerBoneRender<@NonNull R>> consumer ) {
        consumer.accept( pBone, this::renderTool );

        for ( GeoBone geoCube : pBone.children() ) { this.registerBoneTree( geoCube, consumer ); }
    }



    @Override
    public void addPerBoneRender(
            @NonNull RenderPassInfo<@NonNull R> renderPassInfo,
            @NonNull BiConsumer<GeoBone, PerBoneRender<@NonNull R>> consumer
    ) {
        Domovoi.AnimationState animationState = renderPassInfo.getOrDefaultGeckolibData(
                DomovoiRenderer.ANIMATION_STATE,
                Domovoi.AnimationState.NONE
        );
        if ( animationState == Domovoi.AnimationState.NONE ) { return; }

        String boneName = switch ( animationState ) {
            case SLEEPING -> MOTH_BONE;
            case SWEEPING -> BROOM_BONE;
            case DUSTING -> DUSTER_BONE;
            default -> null;
        };
        if ( boneName == null ) { return; }

        renderPassInfo.model().getBone( boneName ).ifPresent( bone -> {
            this.registerBoneTree( bone, consumer );
        } );
    }
}
