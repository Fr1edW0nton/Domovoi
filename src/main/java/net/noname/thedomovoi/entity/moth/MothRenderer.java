package net.noname.thedomovoi.entity.moth;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.noname.thedomovoi.TheDomovoi;
import org.jspecify.annotations.NonNull;

public class MothRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<Moth, @NonNull R> {

    public MothRenderer( EntityRendererProvider.Context context, GeoModel<Moth> model ) { super(context, model); }



    @Override
    public void scaleModelForRender(
            @NonNull RenderPassInfo<@NonNull R> renderPassInfo,
            float widthScale,
            float heightScale
    ) {
        widthScale  *= 1.5F;
        heightScale *= 1.5F;

        super.scaleModelForRender( renderPassInfo, widthScale, heightScale );
    }
}
