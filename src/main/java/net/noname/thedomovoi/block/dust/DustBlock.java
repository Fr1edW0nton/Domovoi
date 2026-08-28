package net.noname.thedomovoi.block.dust;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

import java.util.function.Function;

public class DustBlock extends Block implements SegmentableBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final Function<BlockState, VoxelShape> shapes;

    public DustBlock( Properties properties ) {
        super( properties );

        this.registerDefaultState(
                this.stateDefinition.any()
                .setValue( FACING, Direction.NORTH )
                .setValue( this.getSegmentAmountProperty(), MIN_SEGMENT )
        );
        this.shapes = this.getShapeForEachState( this.getShapeCalculator( FACING, this.getSegmentAmountProperty() ) );
    }



    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                FACING,
                this.getSegmentAmountProperty()
        );
    }



    @Override
    public double getShapeHeight() {
        return 1.0;
    }

    @Override
    protected @NonNull VoxelShape getShape(
            @NonNull BlockState state,
            @NonNull BlockGetter level,
            @NonNull BlockPos pos,
            @NonNull CollisionContext context
    ) {
        return this.shapes.apply( state );
    }



    protected boolean canSurvive( @NonNull BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        return level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP);
    }



    public static BlockState addSegment(BlockState state) {
        if ( !( state.getBlock() instanceof DustBlock dust ) ) { return state; }

        IntegerProperty amount = dust.getSegmentAmountProperty();
        int current = state.getValue( amount );

        if (current >= MAX_SEGMENT) { return state; }

        return state.setValue( amount, current + 1 );
    }
}
