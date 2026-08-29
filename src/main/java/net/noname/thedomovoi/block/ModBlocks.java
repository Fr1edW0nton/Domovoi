package net.noname.thedomovoi.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.noname.thedomovoi.TheDomovoi;
import net.noname.thedomovoi.block.dust.DustBlock;
import net.noname.thedomovoi.block.hearth.DomovoiHearthBlock;
import net.noname.thedomovoi.block.hearth.DomovoiHearthBlockEntity;
import net.noname.thedomovoi.block.hearth.DomovoiHearthBlockItem;
import net.noname.thedomovoi.block.offeringcup.OfferingCupBlock;
import net.noname.thedomovoi.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks( TheDomovoi.MOD_ID );

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES
            = DeferredRegister.create( Registries.BLOCK_ENTITY_TYPE, TheDomovoi.MOD_ID );


    public static final DeferredBlock<DomovoiHearthBlock> DOMOVOI_HEARTH_BLOCK = BLOCKS.register(
            "domovoi_hearth",
            registryName -> new DomovoiHearthBlock(
                    true,
                    3,
                    BlockBehaviour.Properties.of()
                            .setId( ResourceKey.create( Registries.BLOCK, registryName ) )
                            .strength( 1.0F )
                            .sound( SoundType.WOOD )
                            .noOcclusion()
            )
    );

    public static final DeferredItem<BlockItem> DOMOVOI_HEARTH_BLOCK_ITEM = ModItems.ITEMS.registerItem(
            "domovoi_hearth",
            properties -> new DomovoiHearthBlockItem(
                    DOMOVOI_HEARTH_BLOCK.get(),
                    properties.useBlockDescriptionPrefix()
            )
    );

    public static final Supplier<BlockEntityType<DomovoiHearthBlockEntity>> DOMOVOI_HEARTH_BLOCK_ENTITY
            = BLOCK_ENTITY_TYPES.register(
                    "domovoi_hearth_block_entity",
                    () -> new BlockEntityType<>(
                            DomovoiHearthBlockEntity::new,
                            DOMOVOI_HEARTH_BLOCK.get()
                    )
    );


    public static final DeferredBlock<OfferingCupBlock> OFFERING_CUP_BLOCK = BLOCKS.register(
            "offering_cup",
            registryName -> new OfferingCupBlock(
                    BlockBehaviour.Properties.of()
                            .setId( ResourceKey.create( Registries.BLOCK, registryName ) )
                            .strength( 0.1F )
                            .noOcclusion()
            )
    );

    public static final DeferredItem<BlockItem> OFFERING_CUP_BLOCK_ITEM = ModItems.ITEMS.registerItem(
            "offering_cup",
            properties -> new BlockItem(
                    OFFERING_CUP_BLOCK.get(),
                    properties.useBlockDescriptionPrefix()
            )
    );


    public static final DeferredBlock<DustBlock> DUST_BLOCK = BLOCKS.register(
            "dust",
            registryName -> new DustBlock(
                    BlockBehaviour.Properties.of()
                            .setId( ResourceKey.create( Registries.BLOCK, registryName ) )
                            .strength( 0.1F )
                            .noOcclusion()
            )
    );



    public static void register(IEventBus eventBus) {
        BLOCKS.register( eventBus );
        BLOCK_ENTITY_TYPES.register( eventBus );
    }
}
