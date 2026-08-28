package net.noname.thedomovoi.block.hearth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

public class DomovoiHearthBlockItem extends BlockItem {
    public DomovoiHearthBlockItem( Block block, Properties properties ) { super(block, properties); }



    @Override
    public @NonNull InteractionResult place(@NonNull BlockPlaceContext placeContext ) {
        Level level = placeContext.getLevel();

        if ( !( level instanceof ServerLevel serverLevel ) )
        { return super.place( placeContext ); }
        if ( !( placeContext.getPlayer() instanceof ServerPlayer serverPlayer) )
        { return super.place( placeContext ); }

        DomovoiHearthSavedData domovoiHearthData = DomovoiHearthSavedData.get( serverLevel );

        if ( domovoiHearthData.hasHearth( serverLevel, serverPlayer.getUUID() ) ) {
            serverPlayer.sendSystemMessage( Component.translatable( "message.thedomovoi.hearth_already_exists" ) );
            return InteractionResult.FAIL;
        }

        return super.place( placeContext );
    }
}
