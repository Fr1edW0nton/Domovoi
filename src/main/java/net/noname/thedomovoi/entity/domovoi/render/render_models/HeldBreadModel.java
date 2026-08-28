package net.noname.thedomovoi.entity.domovoi.render.render_models;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class HeldBreadModel extends Model<LivingEntityRenderState> {

    private final ModelPart bone;

    public HeldBreadModel( ModelPart root ) {
        super( root, RenderTypes::entityCutout );

        this.bone = root.getChild("bone");
    }



    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.2181F, 22.4599F, -0.5837F, -0.0057F, 0.6366F, -0.1778F));

        PartDefinition bread_r1 = bone.addOrReplaceChild("bread_r1", CubeListBuilder.create().texOffs(28, 32).addBox(-2.6F, -1.225F, -1.475F, 5.95F, 2.45F, 2.95F, new CubeDeformation(0.0F))
                .texOffs(17, 23).addBox(-2.3F, -0.925F, -1.675F, 5.35F, 1.85F, 0.55F, new CubeDeformation(0.0F))
                .texOffs(20, 26).addBox(-2.3F, -0.925F, 1.225F, 5.35F, 1.85F, 0.65F, new CubeDeformation(0.0F))
                .texOffs(2, 28).addBox(-2.3F, -1.425F, -1.175F, 5.35F, 1.05F, 2.35F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2819F, 0.2151F, 0.2587F, -0.1745F, 0.9425F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }
}
