package net.noname.thedomovoi.entity.domovoi.render.render_models;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class HeldBugModel extends Model<LivingEntityRenderState> {
    private final ModelPart root;
    private final ModelPart body;

    public HeldBugModel( ModelPart root ) {
        super( root, RenderTypes::entityCutout );

        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
    }


    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(-0.6F, 23.7F, -1.1F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.6F, -3.3F, -2.1F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(3, 12).addBox(-2.6F, -2.2F, -2.6F, 3.0F, 1.9F, 0.5F, new CubeDeformation(0.0F))
                .texOffs(6, 8).addBox(-2.0F, -3.8F, -1.9F, 0.4F, 0.6F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(8, 8).addBox(-0.6F, -3.8F, -1.9F, 0.4F, 0.6F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(2.2F, 0.0F, 1.2F));

        PartDefinition leg_RF_bone = body.addOrReplaceChild("leg_RF_bone", CubeListBuilder.create(), PartPose.offset(-2.2F, 0.0F, -1.2F));

        PartDefinition leg_RF_r1 = leg_RF_bone.addOrReplaceChild("leg_RF_r1", CubeListBuilder.create().texOffs(5, 8).addBox(-0.2F, -0.3F, 0.0F, 0.4F, 0.9F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.3F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition leg_RB_bone = body.addOrReplaceChild("leg_RB_bone", CubeListBuilder.create(), PartPose.offset(-2.2F, 0.0F, 0.0F));

        PartDefinition leg_RB_r1 = leg_RB_bone.addOrReplaceChild("leg_RB_r1", CubeListBuilder.create().texOffs(6, 8).addBox(-0.2F, -0.3F, 0.0F, 0.4F, 0.9F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.3F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition leg_LF_bone = body.addOrReplaceChild("leg_LF_bone", CubeListBuilder.create(), PartPose.offset(0.0F, -0.3F, -1.2F));

        PartDefinition leg_LF_r1 = leg_LF_bone.addOrReplaceChild("leg_LF_r1", CubeListBuilder.create().texOffs(8, 8).addBox(-0.2F, -0.6F, 0.0F, 0.4F, 0.9F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.3F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition leg_LB_bone = body.addOrReplaceChild("leg_LB_bone", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition leg_LB_r1 = leg_LB_bone.addOrReplaceChild("leg_LB_r1", CubeListBuilder.create().texOffs(7, 8).addBox(-0.2F, -0.3F, 0.0F, 0.4F, 0.9F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.3F, 0.0F, 0.0F, -1.5708F, 0.0F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }
}
