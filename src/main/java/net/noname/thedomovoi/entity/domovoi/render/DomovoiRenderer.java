package net.noname.thedomovoi.entity.domovoi.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.noname.thedomovoi.entity.domovoi.Domovoi;
import net.noname.thedomovoi.entity.domovoi.render.render_layers.DomovoiItemInHandLayer;
import net.noname.thedomovoi.entity.domovoi.render.render_layers.DomovoiToolLayer;
import net.noname.thedomovoi.entity.domovoi.render.render_models.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class DomovoiRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<Domovoi, @NonNull R> {

    public static final DataTicket<Float> REVEAL_PROGRESS
            = DataTicket.create( "domovoi_reveal_progress", Float.class );

    public static final DataTicket<Domovoi.AnimationState> ANIMATION_STATE
            = DataTicket.create( "domovoi_animation_state", Domovoi.AnimationState.class );

    public static final DataTicket<Domovoi.ConsumeType> CONSUME_TYPE
            = DataTicket.create( "domovoi_consume_type", Domovoi.ConsumeType.class );

    public static final DataTicket<Integer> CONSUME_MOB_VARIANT
            = DataTicket.create( "domovoi_consume_mob_variant", Integer.class );

    private static final String BROOM_BONE = "broom";
    private static final String DUSTER_BONE = "duster";
    private static final String MOTH_BONE = "moth";

    public DomovoiRenderer( EntityRendererProvider.Context context, GeoModel<Domovoi> model ) {
        super( context, model );

        HeldMothModel mothModel = new HeldMothModel( context.bakeLayer( HeldModelLayerLocations.MOTH_HELD_ITEM ) );
        HeldBugModel bugModel   = new HeldBugModel( context.bakeLayer( HeldModelLayerLocations.BUG_HELD_ITEM ) );
        HeldMilkModel milkModel   = new HeldMilkModel( context.bakeLayer( HeldModelLayerLocations.MILK_HELD_ITEM ) );
        HeldBreadModel breadModel   = new HeldBreadModel( context.bakeLayer( HeldModelLayerLocations.BREAD_HELD_ITEM ) );

        this.withRenderLayer( new DomovoiToolLayer<>( this ) );
        this.withRenderLayer( new DomovoiItemInHandLayer<>(
                this,
                mothModel,
                bugModel,
                milkModel,
                breadModel
        ) );
    }



    @Override
    public void addRenderData(
            @NonNull Domovoi animatable,
            @Nullable Void relatedObject,
            @NonNull R renderState,
            float partialTick
    ) {
        renderState.addGeckolibData( REVEAL_PROGRESS,       animatable.getRevealProgress() );
        renderState.addGeckolibData( ANIMATION_STATE,       animatable.getAnimationState() );
        renderState.addGeckolibData( CONSUME_TYPE,          animatable.getConsumeType() );
        renderState.addGeckolibData( CONSUME_MOB_VARIANT,   animatable.getConsumeMobVariant() );
    }



    @Override
    public void preRenderPass(
            @NonNull RenderPassInfo<@NonNull R> renderPassInfo,
            @NonNull SubmitNodeCollector renderTasks
    ) {
        if ( !renderPassInfo.willRender() ) { return; }

        renderPassInfo.addBoneUpdater( ( _, snapshot ) -> {
            snapshot.get( BROOM_BONE ).ifPresent( boneSnapshot -> {
                boneSnapshot.skipRender( true );
                boneSnapshot.skipChildrenRender( true );
            } );

            snapshot.get( DUSTER_BONE ).ifPresent( boneSnapshot -> {
                boneSnapshot.skipRender( true );
                boneSnapshot.skipChildrenRender( true );
            } );

            snapshot.get( MOTH_BONE ).ifPresent( boneSnapshot -> {
                boneSnapshot.skipRender( true );
                boneSnapshot.skipChildrenRender( true );
            } );
        } );
    }



    @Override
    public @Nullable RenderType getRenderType( @NonNull R renderState, @NonNull Identifier texture ) {
        float revealProgress = renderState.getOrDefaultGeckolibData( REVEAL_PROGRESS, 1.0F );

        if ( revealProgress <= 0.001F ) { return null; }
        if ( revealProgress < 1.0F ) { return RenderTypes.entityTranslucent( texture, false ); }

        return super.getRenderType( renderState, texture );
    }

    @Override
    public int getRenderColor( @NonNull Domovoi animatable, @Nullable Void relatedObject, float partialTick ) {
        int color =  super.getRenderColor(animatable, relatedObject, partialTick);

        float revealProgress = animatable.getRevealProgressWithPartialTick( partialTick );

        return ARGB.color(
                Mth.clamp( revealProgress, 0.0F, 1.0F ),
                color
        );
    }



    @Override
    public void scaleModelForRender(
            @NonNull RenderPassInfo<@NonNull R> renderPassInfo,
            float widthScale,
            float heightScale
    ) {
        widthScale  *= 0.75F;
        heightScale *= 0.75F;

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }
}
