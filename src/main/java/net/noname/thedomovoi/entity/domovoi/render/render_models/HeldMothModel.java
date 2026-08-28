package net.noname.thedomovoi.entity.domovoi.render.render_models;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.noname.thedomovoi.entity.domovoi.render.DomovoiRenderState;

import java.util.function.Function;

public class HeldMothModel extends Model<LivingEntityRenderState> {

    private final ModelPart root;
    private final ModelPart body;

    public HeldMothModel( ModelPart root ) {
        super( root, RenderTypes::entityCutout );

        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
    }


    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 23.75F, -0.75F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.001F)), PartPose.offset(0.0F, 0.25F, 0.75F));

        PartDefinition leg_RB_r1 = body.addOrReplaceChild("leg_RB_r1", CubeListBuilder.create().texOffs(0, 8).addBox(-0.25F, -0.25F, -1.0F, 0.5F, 0.5F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, -0.25F, -0.75F, 0.2182F, 0.3491F, 0.0F));

        PartDefinition leg_RF_r1 = body.addOrReplaceChild("leg_RF_r1", CubeListBuilder.create().texOffs(4, 8).addBox(-0.25F, -0.25F, -1.0F, 0.5F, 0.5F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.75F, -0.25F, -0.75F, 0.2182F, -0.3927F, 0.0F));

        PartDefinition leg_LB_r1 = body.addOrReplaceChild("leg_LB_r1", CubeListBuilder.create().texOffs(7, 6).addBox(-0.25F, -0.25F, 0.0F, 0.5F, 0.5F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, -0.25F, 0.75F, -0.2182F, -0.3927F, 0.0F));

        PartDefinition leg_LM_r1 = body.addOrReplaceChild("leg_LM_r1", CubeListBuilder.create().texOffs(5, 4).mirror().addBox(-0.25F, -0.25F, 0.0F, 0.5F, 0.5F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -0.25F, 0.75F, -0.0873F, 0.0F, 0.0F));

        PartDefinition leg_LF_r1 = body.addOrReplaceChild("leg_LF_r1", CubeListBuilder.create().texOffs(5, 6).addBox(-0.25F, -0.25F, 0.0F, 0.5F, 0.5F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.75F, -0.25F, 0.75F, -0.2182F, 0.3927F, 0.0F));

        PartDefinition leg_RM_r1 = body.addOrReplaceChild("leg_RM_r1", CubeListBuilder.create().texOffs(2, 8).addBox(-0.25F, -0.25F, -1.0F, 0.5F, 0.5F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.25F, -0.75F, 0.1309F, 0.0F, 0.0F));

        PartDefinition antenna_R = body.addOrReplaceChild("antenna_R", CubeListBuilder.create().texOffs(1, 2).addBox(0.0F, -1.0F, -0.1F, 0.25F, 0.25F, 0.25F, new CubeDeformation(0.0F))
                .texOffs(4, 0).addBox(0.0F, -1.0F, -0.1F, 0.0F, 1.0F, 0.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.25F, -1.75F, -0.35F, 0.1719F, -0.0302F, 0.1719F));

        PartDefinition antenna_L = body.addOrReplaceChild("antenna_L", CubeListBuilder.create().texOffs(8, 8).addBox(0.0F, -1.0F, -0.15F, 0.25F, 0.25F, 0.25F, new CubeDeformation(0.0F))
                .texOffs(6, 0).addBox(0.0F, -1.0F, -0.15F, 0.0F, 1.0F, 0.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.25F, -1.75F, 0.35F, -0.1719F, 0.0302F, 0.1719F));

        PartDefinition wing_R_bone = body.addOrReplaceChild("wing_R_bone", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, 0.0F));

        PartDefinition wing_R_r1 = wing_R_bone.addOrReplaceChild("wing_R_r1", CubeListBuilder.create().texOffs(-4, 12).addBox(-1.75F, 0.0F, 0.0F, 4.0F, 0.0F, 4.0F, new CubeDeformation(0.001F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.8798F, 0.0F, 3.1416F));

        PartDefinition wing_L_bone = body.addOrReplaceChild("wing_L_bone", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, 0.0F));

        PartDefinition wing_L_r1 = wing_L_bone.addOrReplaceChild("wing_L_r1", CubeListBuilder.create().texOffs(-4, 12).addBox(-1.75F, 0.0F, -1.0F, 4.0F, 0.0F, 4.0F, new CubeDeformation(0.001F)), PartPose.offsetAndRotation(-0.5F, -0.3F, 1.0F, 0.2618F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }
}
