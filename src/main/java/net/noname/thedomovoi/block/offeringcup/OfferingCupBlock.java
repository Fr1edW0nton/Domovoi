package net.noname.thedomovoi.block.offeringcup;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

public class OfferingCupBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(
            5.0, 0.0, 5.0,
            11.0, 9.0, 11.0
    );

    public static final BooleanProperty HAS_MILK    = BooleanProperty.create( "has_milk" );
    public static final BooleanProperty HAS_BREAD   = BooleanProperty.create( "has_bread" );

    public static final BooleanProperty IS_VISIBLE  = BooleanProperty.create( "has_milk" );


    public OfferingCupBlock( Properties properties ) {
        super( properties );

        registerDefaultState(
                this.stateDefinition.any()
                        .setValue( HAS_MILK, false )
                        .setValue( HAS_BREAD, false )
                        .setValue( IS_VISIBLE, true )
        );
    }



    @Override
    protected void createBlockStateDefinition( StateDefinition.@NonNull Builder<Block, BlockState> builder ) {
        builder.add(
                HAS_MILK,
                HAS_BREAD,
                IS_VISIBLE
        );
    }



    @Override
    protected @NonNull VoxelShape getShape(
            @NonNull BlockState state,
            @NonNull BlockGetter level,
            @NonNull BlockPos pos,
            @NonNull CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected @NonNull VoxelShape getCollisionShape(
            @NonNull BlockState state,
            @NonNull BlockGetter level,
            @NonNull BlockPos pos,
            @NonNull CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected @NonNull RenderShape getRenderShape(BlockState state) {
        return state.getValue( IS_VISIBLE ) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }



    public static boolean getHasMilk(Level pLevel, BlockPos pBlockPos ) {
        BlockState state = pLevel.getBlockState( pBlockPos );
        return state.hasProperty( HAS_MILK ) && state.getValue( HAS_MILK );
    }

    public static void setHasMilk( Level pLevel, BlockPos pBlockPos, boolean pValue ) {
        BlockState state = pLevel.getBlockState( pBlockPos );
        if ( !state.hasProperty( HAS_MILK ) ) { return; }

        pLevel.setBlock( pBlockPos, state.setValue( HAS_MILK, pValue ), UPDATE_ALL );
    }


    public static boolean getHasBread( Level pLevel, BlockPos pBlockPos ) {
        BlockState state = pLevel.getBlockState( pBlockPos );
        return state.hasProperty( HAS_BREAD ) && state.getValue( HAS_BREAD );
    }

    public static void setHasBread( Level pLevel, BlockPos pBlockPos, boolean pValue  ) {
        BlockState state = pLevel.getBlockState( pBlockPos );
        if ( !state.hasProperty( HAS_BREAD ) ) { return; }

        pLevel.setBlock( pBlockPos, state.setValue( HAS_BREAD, pValue ), UPDATE_ALL );
    }


    public static boolean getIsVisible( Level pLevel, BlockPos pBlockPos ) {
        BlockState state = pLevel.getBlockState( pBlockPos );
        return state.hasProperty( IS_VISIBLE ) && state.getValue( IS_VISIBLE );
    }

    public static void setIsVisible( Level pLevel, BlockPos pBlockPos, boolean pValue ) {
        BlockState state = pLevel.getBlockState( pBlockPos );
        if ( !state.hasProperty( IS_VISIBLE ) ) { return; }

        pLevel.setBlock( pBlockPos, state.setValue( IS_VISIBLE, pValue ), UPDATE_ALL );
    }



    @Override
    protected @NonNull InteractionResult useItemOn(
            @NonNull ItemStack itemStack,
            @NonNull BlockState state,
            @NonNull Level level,
            @NonNull BlockPos pos,
            @NonNull Player player,
            @NonNull InteractionHand hand,
            @NonNull BlockHitResult hitResult
    ) {
        if ( itemStack.is( Items.MILK_BUCKET ) && !state.getValue( HAS_MILK ) ) {
            if ( !level.isClientSide() ) {
                level.setBlock( pos, state.setValue( HAS_MILK, true ), UPDATE_ALL );

                if ( !player.getAbilities().instabuild ) {
                    player.setItemInHand( hand, Items.BUCKET.getDefaultInstance() );
                }

                level.playSound(
                        null,
                        pos,
                        SoundEvents.BUCKET_EMPTY,
                        SoundSource.BLOCKS,
                        1.0f,
                        1.0f
                );
            }
            return InteractionResult.SUCCESS;
        }

        if ( itemStack.is( Items.BREAD ) && !state.getValue( HAS_BREAD ) ) {
            if ( !level.isClientSide() ) {
                level.setBlock( pos, state.setValue( HAS_BREAD, true ), UPDATE_ALL );

                itemStack.shrink( 1 );

                level.playSound(
                        null,
                        pos,
                        SoundEvents.ITEM_FRAME_ADD_ITEM,
                        SoundSource.BLOCKS,
                        1.0f,
                        1.0f
                );
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
