package net.noname.thedomovoi.entity.bug;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jspecify.annotations.NonNull;

public class BugRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<Bug, @NonNull R> {

    public BugRenderer( EntityRendererProvider.Context context, GeoModel<Bug> model ) { super( context, model ); }



    @Override
    public void scaleModelForRender(
            @NonNull RenderPassInfo<@NonNull R> renderPassInfo,
            float widthScale,
            float heightScale
    ) {
        widthScale  *= 0.7F;
        heightScale *= 0.7F;

        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
    }
}
