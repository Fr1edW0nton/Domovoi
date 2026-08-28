package net.noname.thedomovoi.entity.domovoi.render.render_models;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class HeldMilkModel extends Model<LivingEntityRenderState> {

    private final ModelPart bone;

    public HeldMilkModel( ModelPart root ) {
        super( root, RenderTypes::entityCutout );

        this.bone = root.getChild("bone");
    }


    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 26).addBox(-10.6F, -0.4F, 5.4F, 5.2F, 0.4F, 5.2F, new CubeDeformation(0.0F))
                .texOffs(18, 22).addBox(-5.6F, -9.0F, 5.0F, 0.6F, 3.9F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(4, 15).addBox(-11.0F, -9.0F, 5.0F, 0.6F, 3.9F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(20, 2).addBox(-10.4F, -9.0F, 10.4F, 4.8F, 3.9F, 0.6F, new CubeDeformation(0.0F))
                .texOffs(20, 17).addBox(-10.4F, -9.0F, 5.0F, 4.8F, 3.9F, 0.6F, new CubeDeformation(0.0F))
                .texOffs(0, 3).addBox(-11.0F, -5.1F, 5.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(12, 11).addBox(-10.5F, -8.35F, 5.5F, 5.0F, 0.25F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 24).addBox(-8.5F, -4.5F, 7.5F, 1.0F, 4.1F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(8.0F, 24.0F, -8.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }
}
