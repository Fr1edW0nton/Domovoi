package net.noname.thedomovoi.entity.domovoi.render.render_models;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.noname.thedomovoi.TheDomovoi;

public class HeldModelLayerLocations {

    public static final ModelLayerLocation MOTH_HELD_ITEM = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(
                    TheDomovoi.MOD_ID,
                    "moth_held_item"
            ),
            "main"
    );

    public static final ModelLayerLocation BUG_HELD_ITEM = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(
                    TheDomovoi.MOD_ID,
                    "bug_held_item"
            ),
            "main"
    );

    public static final ModelLayerLocation MILK_HELD_ITEM = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(
                    TheDomovoi.MOD_ID,
                    "milk_held_item"
            ),
            "main"
    );

    public static final ModelLayerLocation BREAD_HELD_ITEM = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(
                    TheDomovoi.MOD_ID,
                    "bread_held_item"
            ),
            "main"
    );
}
