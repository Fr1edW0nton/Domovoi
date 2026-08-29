package net.noname.thedomovoi.block.hearth;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.noname.thedomovoi.TheDomovoi;
import net.noname.thedomovoi.block.ModBlocks;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class DomovoiHearthBlock extends CampfireBlock {

    public DomovoiHearthBlock(boolean spawnParticles, int fireDamage, Properties properties ) {
        super( spawnParticles, fireDamage, properties );
    }



    @Override
    public @NonNull BlockEntity newBlockEntity( @NonNull BlockPos worldPosition, @NonNull BlockState blockState ) {
        return new DomovoiHearthBlockEntity( worldPosition, blockState );
    }



    @Override
    public void setPlacedBy(
            @NonNull Level level,
            @NonNull BlockPos pos,
            @NonNull BlockState state,
            @Nullable LivingEntity by,
            @NonNull ItemStack itemStack
    ) {
        super.setPlacedBy(level, pos, state, by, itemStack);

        if (
                level instanceof ServerLevel serverLevel
                && by instanceof ServerPlayer serverPlayer
        ) {
            UUID uuid = serverPlayer.getUUID();

            if ( serverLevel.getBlockEntity( pos ) instanceof DomovoiHearthBlockEntity domovoiHearthBlockEntity ) {
                domovoiHearthBlockEntity.setOwnerUUID( uuid );
            }

            DomovoiHearthSavedData domovoiHearthData = DomovoiHearthSavedData.get( serverLevel );
            domovoiHearthData.setHearth( uuid, pos );

            serverPlayer.sendSystemMessage( Component.translatable( "message.thedomovoi.hearth_placed" ) );
        }
    }



    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NonNull Level level,
            @NonNull BlockState blockState,
            @NonNull BlockEntityType<T> type
    ) {
        return createTickerHelper( type, ModBlocks.DOMOVOI_HEARTH_BLOCK_ENTITY.get(), DomovoiHearthBlockEntity::tick );
    }




}
